package com.clothingstore.clothing_store_be;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.cors.allowed-origins=" + CatalogApiIntegrationTests.ALLOWED_ORIGIN,
        "store.currency=ILS"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CatalogSlugValidationApiIntegrationTests {

    private static final Date CREATED = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
    private static final String MAX_LENGTH_SLUG = "a-" + "b".repeat(198);
    private static final String TOO_LONG_SLUG = "c-" + "d".repeat(199);
    private static final String UNICODE_SLUG = "mén";
    private static final String MIXED_CASE_SLUG = "Mixed-Case";
    private static final ObjectId MAX_LENGTH_CATEGORY = new ObjectId("690000000000000000000a01");
    private static final ObjectId TOO_LONG_CATEGORY = new ObjectId("690000000000000000000a02");
    private static final ObjectId MIXED_CASE_CATEGORY = new ObjectId("690000000000000000000a03");
    private static final ObjectId UNICODE_CATEGORY = new ObjectId("690000000000000000000a04");

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());
        mongoTemplate.getCollection("product_variants").deleteMany(new Document());
        mongoTemplate.getCollection("categories").insertMany(List.of(
                category(MAX_LENGTH_CATEGORY, MAX_LENGTH_SLUG),
                category(TOO_LONG_CATEGORY, TOO_LONG_SLUG),
                category(MIXED_CASE_CATEGORY, MIXED_CASE_SLUG),
                category(UNICODE_CATEGORY, UNICODE_SLUG)));
        mongoTemplate.getCollection("products").insertMany(List.of(
                product(new ObjectId("690000000000000000000b01"), MAX_LENGTH_SLUG, MAX_LENGTH_CATEGORY, true),
                product(new ObjectId("690000000000000000000b02"), TOO_LONG_SLUG, TOO_LONG_CATEGORY, false),
                product(new ObjectId("690000000000000000000b03"), MIXED_CASE_SLUG, MIXED_CASE_CATEGORY, false),
                product(new ObjectId("690000000000000000000b04"), UNICODE_SLUG, UNICODE_CATEGORY, false)));
    }

    private static Document category(ObjectId id, String slug) {
        return new Document("_id", id).append("slug", slug).append("name", "Hidden Category " + id.toHexString())
                .append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private static Document product(ObjectId id, String slug, ObjectId categoryId, boolean featured) {
        return new Document("_id", id).append("slug", slug).append("name", "Hidden Product " + id.toHexString())
                .append("description", "Description").append("categoryId", categoryId)
                .append("basePrice", Decimal128.parse("10.00")).append("images", List.of())
                .append("active", true).append("featured", featured)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private CartApiTestResponse get(String pathAndQuery) {
        return new CartApiTestClient(port).sendWithHeaders("GET", pathAndQuery, null);
    }

    private static void assertNotFound(CartApiTestResponse response, String code) {
        assertThat(response.status()).as(response.body()).isEqualTo(404);
        assertThat(response.header("Content-Type")).startsWith("application/problem+json");
        assertThat(response.<String>read("$.code")).isEqualTo(code);
        assertThat(response.body()).doesNotContain("Exception").doesNotContain("java.").doesNotContain("Hidden");
    }

    private static void assertSameProblem(CartApiTestResponse malformed, CartApiTestResponse unknown) {
        assertThat(malformed.status()).isEqualTo(unknown.status());
        assertThat(malformed.body()).isEqualTo(unknown.body());
    }

    @Test
    void categorySlugAtConfiguredMaximumLengthIsFound() {
        CartApiTestResponse response = get("/api/v1/categories/" + MAX_LENGTH_SLUG);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.<String>read("$.slug")).isEqualTo(MAX_LENGTH_SLUG);
    }

    @Test
    void storedCategorySlugOneCharacterOverMaximumIsNotFound() {
        assertNotFound(get("/api/v1/categories/" + TOO_LONG_SLUG), "CATEGORY_NOT_FOUND");
    }

    @Test
    void productSlugAtConfiguredMaximumLengthIsFound() {
        CartApiTestResponse response = get("/api/v1/products/" + MAX_LENGTH_SLUG);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.<String>read("$.slug")).isEqualTo(MAX_LENGTH_SLUG);
    }

    @Test
    void storedProductSlugOneCharacterOverMaximumIsNotFound() {
        assertNotFound(get("/api/v1/products/" + TOO_LONG_SLUG), "PRODUCT_NOT_FOUND");
    }

    @Test
    void categoryFilterAtConfiguredMaximumLengthFiltersProducts() {
        CartApiTestResponse response = get("/api/v1/products?category=" + MAX_LENGTH_SLUG);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.<List<String>>read("$.items[*].slug")).containsExactly(MAX_LENGTH_SLUG);
    }

    @Test
    void storedCategoryFilterOneCharacterOverMaximumIsNotFound() {
        assertNotFound(get("/api/v1/products?category=" + TOO_LONG_SLUG), "CATEGORY_NOT_FOUND");
    }

    @Test
    void storedSlugsThatAreNotLowercaseKebabCaseAreNeverServed() {
        for (String slug : List.of(MIXED_CASE_SLUG, "m%C3%A9n")) {
            assertNotFound(get("/api/v1/categories/" + slug), "CATEGORY_NOT_FOUND");
            assertNotFound(get("/api/v1/products/" + slug), "PRODUCT_NOT_FOUND");
            assertNotFound(get("/api/v1/products?category=" + slug), "CATEGORY_NOT_FOUND");
        }
    }

    @ParameterizedTest(name = "encoded={0}")
    @ValueSource(strings = {"m%C3%A9n", "%EF%BD%8Den", "%D9%A3", "men%E2%80%8B", "men%09", "men%0A", "men%0D%0A",
            "%20men", "men%2Eshirt", "men%2Bshirt", "men%25", "%C4%B1", "a%CC%81"})
    void percentEncodedNonKebabCaseSlugReturnsSameProblemAsUnknownSlug(String encodedSlug) {
        assertSameProblem(get("/api/v1/categories/" + encodedSlug), get("/api/v1/categories/does-not-exist"));
        assertSameProblem(get("/api/v1/products/" + encodedSlug), get("/api/v1/products/does-not-exist"));
        assertSameProblem(get("/api/v1/products?category=" + encodedSlug), get("/api/v1/products?category=nope"));
    }

    @ParameterizedTest(name = "encoded={0}")
    @ValueSource(strings = {"men%2Fshirt", "men%5Cshirt", "men%00", "%2E%2E"})
    void reservedOrControlCharactersInSlugPathNeverCauseServerErrors(String encodedSlug) {
        for (String path : List.of("/api/v1/categories/" + encodedSlug, "/api/v1/products/" + encodedSlug)) {
            CartApiTestResponse response = get(path);

            assertThat(response.status()).as(path).isBetween(400, 499);
            assertThat(response.header("Content-Type")).as(path).startsWith("application/problem+json");
            assertThat(response.body()).as(path).doesNotContain("Exception").doesNotContain("Tomcat");
        }
    }

    @ParameterizedTest(name = "category={0}")
    @ValueSource(strings = {"Mixed-Case", "m%C3%A9n", "men_wear", ""})
    void malformedCategoryFilterCombinedWithValidFiltersReturnsSameProblemAsUnknownCategory(String category) {
        String filters = "&featured=true&sort=price_asc&page=0&size=5";

        assertSameProblem(get("/api/v1/products?category=" + category + filters),
                get("/api/v1/products?category=nope" + filters));
    }

    @Test
    void invalidPaginationIsReportedBeforeMalformedCategoryAndOnlyNamesThePaginationFields() {
        CartApiTestResponse response = get("/api/v1/products?category=Mixed-Case&page=-1&size=0");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.<String>read("$.code")).isEqualTo("VALIDATION_ERROR");
        assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly("page", "size");
    }

    @Test
    void repeatedCategoryParameterIsNotFoundRatherThanMatchingEitherValue() {
        assertNotFound(get("/api/v1/products?category=" + MAX_LENGTH_SLUG + "&category=" + MAX_LENGTH_SLUG),
                "CATEGORY_NOT_FOUND");
    }

    @Test
    void unknownQueryParametersAreIgnored() {
        CartApiTestResponse plain = get("/api/v1/products?category=" + MAX_LENGTH_SLUG);
        CartApiTestResponse withExtras = get("/api/v1/products?category=" + MAX_LENGTH_SLUG
                + "&price=0&active=false&debug=true&%24where=1");

        assertThat(withExtras.status()).isEqualTo(200);
        assertThat(withExtras.body()).isEqualTo(plain.body());
        assertThat(get("/api/v1/categories/" + MAX_LENGTH_SLUG + "?active=false").status()).isEqualTo(200);
        assertThat(get("/api/v1/products/" + MAX_LENGTH_SLUG + "?active=false&price=0").<String>read("$.price.amount"))
                .isEqualTo("10.00");
        assertThat(get("/api/v1/categories?includeInactive=true").<List<String>>read("$[*].slug"))
                .contains(MAX_LENGTH_SLUG);
    }
}
