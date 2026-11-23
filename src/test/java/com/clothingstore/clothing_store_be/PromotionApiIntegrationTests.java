package com.clothingstore.clothing_store_be;

import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PromotionApiIntegrationTests {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void clearPromotions() {
        mongoTemplate.getCollection("promotions").deleteMany(new Document());
    }

    private void insertPromotion(String title, String subtitle, String ctaLabel, String ctaUrl, int displayOrder,
            boolean active, Instant startsAt, Instant endsAt) {
        Date created = Date.from(Instant.now().minus(Duration.ofDays(30)));
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", title).append("subtitle", subtitle)
                .append("imageUrl", "/images/promotions/new-season.svg").append("imageAlt", "Alt " + title)
                .append("ctaLabel", ctaLabel).append("ctaUrl", ctaUrl)
                .append("displayOrder", displayOrder).append("active", active)
                .append("startsAt", startsAt == null ? null : Date.from(startsAt))
                .append("endsAt", endsAt == null ? null : Date.from(endsAt))
                .append("createdAt", created).append("updatedAt", created));
    }

    private HttpResponse<String> get(String path) {
        return send("GET", path);
    }

    private HttpResponse<String> send(String method, String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    void returnsOnlyCurrentlyActivePromotionsSortedWithSafeCallToActions() {
        Instant now = Instant.now();
        insertPromotion("Accessories edit", "Finishing touches for every outfit.", "Shop accessories",
                "/categories/accessories", 2, true, null, null);
        insertPromotion("New season arrivals", null, "Shop women", "/categories/women", 1, true,
                now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        insertPromotion("External link", "Sub", "Go", "https://evil.example", 3, true, null, null);
        insertPromotion("Archived campaign", "Sub", null, null, 0, false, null, null);
        insertPromotion("Upcoming", "Sub", null, null, 0, true, now.plus(Duration.ofDays(1)), null);
        insertPromotion("Expired", "Sub", null, null, 0, true, null, now.minus(Duration.ofDays(1)));

        HttpResponse<String> response = get("/api/v1/promotions");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).get().asString().startsWith("application/json");
        assertThat(JsonPath.<List<String>>read(response.body(), "$[*].title"))
                .containsExactly("New season arrivals", "Accessories edit", "External link");
        assertThat(JsonPath.<String>read(response.body(), "$[0].ctaUrl")).isEqualTo("/categories/women");
        assertThat(JsonPath.<String>read(response.body(), "$[0].ctaLabel")).isEqualTo("Shop women");
        assertThat(JsonPath.<String>read(response.body(), "$[0].subtitle")).isNull();
        assertThat(JsonPath.<String>read(response.body(), "$[0].imageAlt")).isEqualTo("Alt New season arrivals");
        assertThat(JsonPath.<String>read(response.body(), "$[2].ctaUrl")).isNull();
        assertThat(JsonPath.<String>read(response.body(), "$[2].ctaLabel")).isNull();
        assertThat(JsonPath.<Map<String, Object>>read(response.body(), "$[0]").keySet())
                .containsExactlyInAnyOrder("id", "title", "subtitle", "imageUrl", "imageAlt", "ctaLabel", "ctaUrl");
    }

    @Test
    void returnsEmptyArrayWhenNoPromotionIsActive() {
        insertPromotion("Archived campaign", null, null, null, 1, false, null, null);

        HttpResponse<String> response = get("/api/v1/promotions");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<Object>>read(response.body(), "$")).isEmpty();
    }

    @Test
    void servesPromotionImagesAsStaticResources() {
        HttpResponse<String> response = get("/images/promotions/new-season.svg");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).get().asString().startsWith("image/svg+xml");
    }

    @Test
    void neverLeaksInternalFieldsAndToleratesUnknownOrMissingDocumentFields() {
        insertPromotion("Full", "Sub", "Shop", "/categories/women", 1, true, null, null);
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", "Sparse").append("imageUrl", "/images/promotions/new-season.svg")
                .append("imageAlt", "Alt Sparse").append("displayOrder", 2).append("active", true)
                .append("internalNote", "secret-internal-note"));
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", "No active flag").append("imageUrl", "/images/promotions/new-season.svg")
                .append("imageAlt", "Alt").append("displayOrder", 0));

        HttpResponse<String> response = get("/api/v1/promotions");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(response.body(), "$[*].title")).containsExactly("Full", "Sparse");
        assertThat(JsonPath.<List<Map<String, Object>>>read(response.body(), "$")).allSatisfy(promotion ->
                assertThat(promotion.keySet()).containsExactlyInAnyOrder(
                        "id", "title", "subtitle", "imageUrl", "imageAlt", "ctaLabel", "ctaUrl"));
        assertThat(JsonPath.<String>read(response.body(), "$[1].subtitle")).isNull();
        assertThat(JsonPath.<String>read(response.body(), "$[1].ctaUrl")).isNull();
        assertThat(response.body()).doesNotContain("secret-internal-note", "displayOrder", "active", "startsAt",
                "endsAt", "createdAt", "updatedAt", "_id");
    }

    @Test
    void ignoresQueryParametersThatTryToWidenTheResult() {
        insertPromotion("Live", null, null, null, 1, true, null, null);
        insertPromotion("Archived campaign", null, null, null, 2, false, null, null);

        HttpResponse<String> response = get("/api/v1/promotions?active=false&includeInactive=true&size=100");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(response.body(), "$[*].title")).containsExactly("Live");
    }

    @Test
    void writeMethodIsRejectedWith405ProblemDetailAndDoesNotChangeData() {
        insertPromotion("Live", null, null, null, 1, true, null, null);

        HttpResponse<String> response = send("DELETE", "/api/v1/promotions");

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Content-Type")).get().asString()
                .startsWith("application/problem+json");
        assertThat(response.headers().firstValue("Allow")).get().asString().contains("GET").doesNotContain("DELETE");
        assertThat(response.body()).doesNotContain("Exception", "java.");
        assertThat(mongoTemplate.getCollection("promotions").countDocuments()).isEqualTo(1);
    }

    @Test
    void headRequestReturnsOkWithoutBody() {
        insertPromotion("Live", null, null, null, 1, true, null, null);

        HttpResponse<String> response = send("HEAD", "/api/v1/promotions");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
    }
}
