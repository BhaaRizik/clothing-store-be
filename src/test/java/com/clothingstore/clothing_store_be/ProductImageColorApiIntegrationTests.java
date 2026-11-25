package com.clothingstore.clothing_store_be;

import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProductImageColorApiIntegrationTests {

    private static final Date CREATED = Date.from(Instant.parse("2026-02-01T00:00:00Z"));
    private static final ObjectId CATEGORY = new ObjectId("670000000000000000000a01");
    private static final ObjectId SHIRT = new ObjectId("670000000000000000000b01");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCatalog() {
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());
        mongoTemplate.getCollection("product_variants").deleteMany(new Document());
        mongoTemplate.getCollection("categories").insertOne(new Document("_id", CATEGORY).append("slug", "tops")
                .append("name", "Tops").append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
    }

    private void insertShirt(List<Document> images, List<String> variantColors) {
        mongoTemplate.getCollection("products").insertOne(new Document("_id", SHIRT).append("slug", "shirt")
                .append("name", "Shirt").append("description", "A shirt").append("categoryId", CATEGORY)
                .append("basePrice", Decimal128.parse("30.00")).append("images", images)
                .append("active", true).append("featured", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        for (int index = 0; index < variantColors.size(); index++) {
            mongoTemplate.getCollection("product_variants").insertOne(new Document("_id", new ObjectId())
                    .append("productId", SHIRT).append("sku", "SHIRT-" + index).append("size", "M")
                    .append("color", new Document("name", variantColors.get(index)).append("hexCode", "#000000"))
                    .append("stockQuantity", 3).append("active", true).append("displayOrder", index)
                    .append("createdAt", CREATED).append("updatedAt", CREATED));
        }
    }

    private static Document image(String url, String alt) {
        return new Document("url", url).append("alt", alt);
    }

    private HttpResponse<String> get(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static <T> T read(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), path);
    }

    @Test
    void imagesTaggedOnlyWithColorsTheProductDoesNotOfferAreReturnedUnfilteredInStoredOrder() {
        insertShirt(List.of(
                image("/img/red.svg", "Red shirt").append("color", "Red"),
                image("/img/green.svg", "Green shirt").append("color", "Green")),
                List.of("Navy", "White"));

        HttpResponse<String> response = get("/api/v1/products/shirt");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(ProductImageColorApiIntegrationTests.<List<String>>read(response, "$.images[*].url"))
                .containsExactly("/img/red.svg", "/img/green.svg");
        assertThat(ProductImageColorApiIntegrationTests.<List<String>>read(response, "$.images[*].color"))
                .containsExactly("Red", "Green");
        assertThat(ProductImageColorApiIntegrationTests.<List<String>>read(response, "$.variants[*].color.name"))
                .containsExactly("Navy", "White");
    }

    @Test
    void mixedTaggedAndUntaggedImagesKeepStoredOrderSoTheFirstImageStaysTheDefault() {
        insertShirt(List.of(
                image("/img/white-front.svg", "White shirt, front").append("color", "White"),
                image("/img/detail.svg", "Fabric detail"),
                image("/img/navy-front.svg", "Navy shirt, front").append("color", "Navy"),
                image("/img/label.svg", "Care label").append("color", null)),
                List.of("White", "Navy"));

        HttpResponse<String> response = get("/api/v1/products/shirt");

        assertThat(ProductImageColorApiIntegrationTests.<List<Map<String, Object>>>read(response, "$.images"))
                .extracting(image -> image.get("url"), image -> image.get("color"))
                .containsExactly(
                        tuple("/img/white-front.svg", "White"),
                        tuple("/img/detail.svg", null),
                        tuple("/img/navy-front.svg", "Navy"),
                        tuple("/img/label.svg", null));
        assertThat(ProductImageColorApiIntegrationTests.<List<Map<String, Object>>>read(response, "$.images"))
                .allSatisfy(image -> assertThat(image).containsOnlyKeys("url", "alt", "color"));
    }

    @Test
    void colorTagIsReturnedVerbatimWithoutCaseOrWhitespaceNormalisation() {
        insertShirt(List.of(
                image("/img/a.svg", "Light blue shirt").append("color", "Light blue"),
                image("/img/b.svg", "Light Blue shirt").append("color", "light Blue ")),
                List.of("Light blue"));

        HttpResponse<String> response = get("/api/v1/products/shirt");

        assertThat(ProductImageColorApiIntegrationTests.<List<String>>read(response, "$.images[*].color"))
                .containsExactly("Light blue", "light Blue ");
    }

    @Test
    void everyCardEndpointVariantExposesPrimaryImageWithoutColorEvenWhenTheFirstImageIsTagged() {
        insertShirt(List.of(image("/img/white-front.svg", "White shirt, front").append("color", "White")),
                List.of("White"));

        for (String path : List.of("/api/v1/products", "/api/v1/products?category=tops",
                "/api/v1/products?featured=true", "/api/v1/products?sort=price_asc")) {
            HttpResponse<String> response = get(path);

            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(ProductImageColorApiIntegrationTests.<Map<String, Object>>read(response, "$.items[0].primaryImage"))
                    .as(path)
                    .isEqualTo(Map.of("url", "/img/white-front.svg", "alt", "White shirt, front"));
            assertThat(response.body()).as(path).doesNotContain("\"color\"");
        }
    }

    @Test
    void openApiDocumentsColorOnDetailImagesButNotOnCardImages() {
        HttpResponse<String> response = get("/v3/api-docs");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(ProductImageColorApiIntegrationTests.<Map<String, Object>>read(response,
                "$.components.schemas.ProductImageResponse.properties")).containsOnlyKeys("url", "alt", "color");
        assertThat(ProductImageColorApiIntegrationTests.<Map<String, Object>>read(response,
                "$.components.schemas.ImageResponse.properties")).containsOnlyKeys("url", "alt");
        assertThat(ProductImageColorApiIntegrationTests.<String>read(response,
                "$.components.schemas.ProductDetailsResponse.properties.images.items.$ref"))
                .endsWith("/ProductImageResponse");
        assertThat(ProductImageColorApiIntegrationTests.<String>read(response,
                "$.components.schemas.ProductCardResponse.properties.primaryImage.$ref"))
                .endsWith("/ImageResponse");
    }

    @Test
    void nonStringColorTagInStoredDataDoesNotBreakTheProductPage() {
        insertShirt(List.of(image("/img/a.svg", "Shirt").append("color", 42)), List.of("White"));

        HttpResponse<String> response = get("/api/v1/products/shirt");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(ProductImageColorApiIntegrationTests.<String>read(response, "$.images[0].url")).isEqualTo("/img/a.svg");
    }
}
