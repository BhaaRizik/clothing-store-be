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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProductVariantsApiIntegrationTests {

    private static final Date CREATED = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
    private static final ObjectId CATEGORY = new ObjectId("660000000000000000000a01");
    private static final ObjectId TEE = new ObjectId("660000000000000000000b01");
    private static final ObjectId HOODIE = new ObjectId("660000000000000000000b02");
    private static final ObjectId RETIRED_VARIANTS_ONLY = new ObjectId("660000000000000000000b03");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());
        mongoTemplate.getCollection("product_variants").deleteMany(new Document());
        mongoTemplate.getCollection("categories").insertOne(new Document("_id", CATEGORY).append("slug", "basics")
                .append("name", "Basics").append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("products").insertMany(List.of(
                product(TEE, "tee"), product(HOODIE, "hoodie"), product(RETIRED_VARIANTS_ONLY, "retired-variants")));
    }

    private static Document product(ObjectId id, String slug) {
        return new Document("_id", id).append("slug", slug).append("name", "Name " + slug)
                .append("description", "Description " + slug).append("categoryId", CATEGORY)
                .append("basePrice", Decimal128.parse("20.00")).append("images", List.of())
                .append("active", true).append("featured", false)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private static Document variant(ObjectId id, ObjectId productId, String sku, Object stockQuantity, boolean active,
            int displayOrder) {
        return new Document("_id", id).append("productId", productId).append("sku", sku)
                .append("size", "M").append("color", new Document("name", "Black").append("hexCode", "#000000"))
                .append("stockQuantity", stockQuantity).append("active", active).append("displayOrder", displayOrder)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private void insertVariants(List<Document> variants) {
        mongoTemplate.getCollection("product_variants").insertMany(variants);
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

    private static String hex(ObjectId id) {
        return id.toHexString();
    }

    @Test
    void availabilityBoundariesForStockQuantityEndToEnd() {
        ObjectId minusFive = new ObjectId("660000000000000000000c01");
        ObjectId minusOne = new ObjectId("660000000000000000000c02");
        ObjectId zero = new ObjectId("660000000000000000000c03");
        ObjectId one = new ObjectId("660000000000000000000c04");
        ObjectId max = new ObjectId("660000000000000000000c05");
        insertVariants(List.of(
                variant(minusFive, TEE, "TEE-1", -5, true, 1),
                variant(minusOne, TEE, "TEE-2", -1, true, 2),
                variant(zero, TEE, "TEE-3", 0, true, 3),
                variant(one, TEE, "TEE-4", 1, true, 4),
                variant(max, TEE, "TEE-5", Integer.MAX_VALUE, true, 5)));

        HttpResponse<String> response = get("/api/v1/products/tee");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(this.<List<String>>readList(response, "$.variants[*].id"))
                .containsExactly(hex(minusFive), hex(minusOne), hex(zero), hex(one), hex(max));
        assertThat(this.<List<Boolean>>readList(response, "$.variants[*].available"))
                .containsExactly(false, false, false, true, true);
        assertThat(response.body()).doesNotContain("2147483647").doesNotContain("-5");
    }

    private <T> T readList(HttpResponse<String> response, String path) {
        return read(response, path);
    }

    @Test
    void stockStoredAsInt64OrDoubleIsInterpretedNumerically() {
        insertVariants(List.of(
                variant(new ObjectId("660000000000000000000c11"), TEE, "TEE-L", 3L, true, 1),
                variant(new ObjectId("660000000000000000000c12"), TEE, "TEE-D0", 0.0d, true, 2),
                variant(new ObjectId("660000000000000000000c13"), TEE, "TEE-D2", 2.0d, true, 3)));

        HttpResponse<String> response = get("/api/v1/products/tee");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(this.<List<Boolean>>readList(response, "$.variants[*].available")).containsExactly(true, false, true);
    }

    @Test
    void variantDocumentWithoutSizeColorOrTimestampFieldsSerializesExplicitNulls() {
        ObjectId bare = new ObjectId("660000000000000000000c21");
        insertVariants(List.of(new Document("_id", bare).append("productId", TEE).append("sku", "TEE-BARE")
                .append("stockQuantity", 0).append("active", true).append("displayOrder", 0)));

        HttpResponse<String> response = get("/api/v1/products/tee");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(this.<Map<String, Object>>readList(response, "$.variants[0]"))
                .containsOnlyKeys("id", "size", "color", "available")
                .containsEntry("id", hex(bare))
                .containsEntry("size", null)
                .containsEntry("color", null)
                .containsEntry("available", false);
    }

    @Test
    void variantDocumentWithoutStockQuantityIsUnavailableInsteadOfFailingTheWholeProduct() {
        ObjectId bare = new ObjectId("660000000000000000000c22");
        insertVariants(List.of(new Document("_id", bare).append("productId", TEE).append("sku", "TEE-NOSTOCK")
                .append("active", true).append("displayOrder", 0)));

        HttpResponse<String> response = get("/api/v1/products/tee");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(this.<Map<String, Object>>readList(response, "$.variants[0]"))
                .containsOnlyKeys("id", "size", "color", "available")
                .containsEntry("id", hex(bare))
                .containsEntry("size", null)
                .containsEntry("color", null)
                .containsEntry("available", false);
    }

    @Test
    void productWhoseVariantsAreAllInactiveHasEmptyVariantsArrayAndNoStore() {
        insertVariants(List.of(
                variant(new ObjectId("660000000000000000000c31"), RETIRED_VARIANTS_ONLY, "RET-1", 10, false, 1),
                variant(new ObjectId("660000000000000000000c32"), RETIRED_VARIANTS_ONLY, "RET-2", 0, false, 2)));

        HttpResponse<String> response = get("/api/v1/products/retired-variants");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(this.<List<Object>>readList(response, "$.variants")).isNotNull().isEmpty();
        assertThat(response.body()).contains("\"variants\":[]").doesNotContain("RET-");
    }

    @Test
    void variantsOfOtherProductsNeverLeakIntoDetails() {
        ObjectId teeVariant = new ObjectId("660000000000000000000c41");
        ObjectId hoodieVariant = new ObjectId("660000000000000000000c42");
        ObjectId orphanVariant = new ObjectId("660000000000000000000c43");
        insertVariants(List.of(
                variant(hoodieVariant, HOODIE, "HOODIE-M", 5, true, 0),
                variant(teeVariant, TEE, "TEE-M", 5, true, 1),
                variant(orphanVariant, new ObjectId(), "ORPHAN-M", 5, true, 0)));

        HttpResponse<String> tee = get("/api/v1/products/tee");
        HttpResponse<String> hoodie = get("/api/v1/products/hoodie");

        assertThat(this.<List<String>>readList(tee, "$.variants[*].id")).containsExactly(hex(teeVariant));
        assertThat(this.<List<String>>readList(hoodie, "$.variants[*].id")).containsExactly(hex(hoodieVariant));
        assertThat(tee.body()).doesNotContain(hex(orphanVariant)).doesNotContain(hex(hoodieVariant));
    }

    @Test
    void variantWithProductIdStoredAsStringInsteadOfObjectIdIsNotMatched() {
        insertVariants(List.of(new Document(variant(new ObjectId("660000000000000000000c51"), TEE, "TEE-STR", 5, true, 0))
                .append("productId", hex(TEE))));

        HttpResponse<String> response = get("/api/v1/products/tee");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(this.<List<Object>>readList(response, "$.variants")).isEmpty();
    }

    @Test
    void sizeOnlyColorOnlyAndHexlessVariantsSerializeExplicitNulls() {
        ObjectId sizeOnly = new ObjectId("660000000000000000000c61");
        ObjectId colorOnly = new ObjectId("660000000000000000000c62");
        ObjectId hexless = new ObjectId("660000000000000000000c63");
        ObjectId hexKeyMissing = new ObjectId("660000000000000000000c64");
        insertVariants(List.of(
                new Document(variant(sizeOnly, TEE, "TEE-SO", 1, true, 1)).append("color", null),
                new Document(variant(colorOnly, TEE, "TEE-CO", 1, true, 2)).append("size", null),
                new Document(variant(hexless, TEE, "TEE-HX", 0, true, 3))
                        .append("color", new Document("name", "Floral print").append("hexCode", null)),
                new Document(variant(hexKeyMissing, TEE, "TEE-HM", 2, true, 4))
                        .append("color", new Document("name", "Camo"))));

        HttpResponse<String> response = get("/api/v1/products/tee");

        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, Object>> variants = read(response, "$.variants");
        assertThat(variants).hasSize(4).allSatisfy(variant ->
                assertThat(variant).containsOnlyKeys("id", "size", "color", "available"));
        assertThat(variants.get(0)).containsEntry("size", "M").containsEntry("color", null);
        assertThat(variants.get(1)).containsEntry("size", null)
                .containsEntry("color", Map.of("name", "Black", "hexCode", "#000000"));
        assertThat(variants.get(2)).containsEntry("available", false);
        assertThat((Map<String, Object>) variants.get(2).get("color")).containsOnlyKeys("name", "hexCode")
                .containsEntry("name", "Floral print").containsEntry("hexCode", null);
        assertThat((Map<String, Object>) variants.get(3).get("color")).containsOnlyKeys("name", "hexCode")
                .containsEntry("name", "Camo").containsEntry("hexCode", null);
    }

    @Test
    void manyVariantsAreOrderedByDisplayOrderThenIdRegardlessOfInsertionOrderAndStableAcrossCalls() {
        List<Document> variants = new ArrayList<>();
        List<Document> expected = new ArrayList<>();
        int[] displayOrders = {5, -1, 0, 5, 3, 0, 5, 100, 3, -1, 0, 7, 5, 2, 2, 50};
        for (int i = 0; i < displayOrders.length; i++) {
            ObjectId id = new ObjectId("6600000000000000000d%04x".formatted((i * 7919) % 65521));
            Document variant = variant(id, TEE, "TEE-ORDER-" + i, i % 3, true, displayOrders[i]);
            variants.add(variant);
            expected.add(variant);
        }
        variants.add(variant(new ObjectId("6600000000000000000dffff"), TEE, "TEE-ORDER-INACTIVE", 9, false, -100));
        Collections.shuffle(variants, new Random(42));
        insertVariants(variants);
        expected.sort(Comparator.<Document>comparingInt(doc -> doc.getInteger("displayOrder"))
                .thenComparing(doc -> doc.getObjectId("_id")));
        List<String> expectedIds = expected.stream().map(doc -> hex(doc.getObjectId("_id"))).toList();

        for (int call = 0; call < 3; call++) {
            HttpResponse<String> response = get("/api/v1/products/tee");
            assertThat(this.<List<String>>readList(response, "$.variants[*].id")).containsExactlyElementsOf(expectedIds);
        }
    }

    @Test
    void stockChangeIsReflectedOnTheNextRequest() {
        ObjectId id = new ObjectId("660000000000000000000c71");
        insertVariants(List.of(variant(id, TEE, "TEE-LIVE", 1, true, 0)));
        assertThat(this.<List<Boolean>>readList(get("/api/v1/products/tee"), "$.variants[*].available")).containsExactly(true);

        mongoTemplate.getCollection("product_variants").updateOne(new Document("_id", id),
                new Document("$set", new Document("stockQuantity", 0)));
        assertThat(this.<List<Boolean>>readList(get("/api/v1/products/tee"), "$.variants[*].available")).containsExactly(false);

        mongoTemplate.getCollection("product_variants").updateOne(new Document("_id", id),
                new Document("$set", new Document("active", false)));
        assertThat(this.<List<Object>>readList(get("/api/v1/products/tee"), "$.variants")).isEmpty();
    }

    @Test
    void notFoundResponseIsNotMarkedNoStore() {
        HttpResponse<String> response = get("/api/v1/products/does-not-exist");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                value -> assertThat(value).startsWith("application/problem+json"));
        assertThat(response.headers().firstValue("Cache-Control")).isEmpty();
        assertThat(read(response, "$.code").toString()).isEqualTo("PRODUCT_NOT_FOUND");
    }
}
