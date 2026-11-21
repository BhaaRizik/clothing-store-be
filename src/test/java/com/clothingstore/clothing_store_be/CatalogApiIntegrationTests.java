package com.clothingstore.clothing_store_be;

import com.clothingstore.clothing_store_be.model.Category;
import com.clothingstore.clothing_store_be.model.Product;
import com.clothingstore.clothing_store_be.model.ProductImage;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.cors.allowed-origins=" + CatalogApiIntegrationTests.ALLOWED_ORIGIN,
        "store.currency=ILS"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CatalogApiIntegrationTests {

    static final String ALLOWED_ORIGIN = "https://shop.example.test";
    static final String DISALLOWED_ORIGIN = "https://evil.example.test";

    private static final Instant BASE = Instant.parse("2026-01-10T00:00:00Z");

    private static final ObjectId MEN = new ObjectId("650000000000000000000a01");
    private static final ObjectId WOMEN = new ObjectId("650000000000000000000a02");
    private static final ObjectId ACCESSORIES = new ObjectId("650000000000000000000a03");
    private static final ObjectId KIDS = new ObjectId("650000000000000000000a04");
    private static final ObjectId ARCHIVE = new ObjectId("650000000000000000000a05");

    private static final ObjectId SHIRT = new ObjectId("650000000000000000000b01");
    private static final ObjectId COAT = new ObjectId("650000000000000000000b02");
    private static final ObjectId SOCKS = new ObjectId("650000000000000000000b03");
    private static final ObjectId RETIRED = new ObjectId("650000000000000000000b04");
    private static final ObjectId DRESS = new ObjectId("650000000000000000000b05");
    private static final ObjectId TEE_B = new ObjectId("650000000000000000000b06");
    private static final ObjectId TEE_A = new ObjectId("650000000000000000000b07");

    private static final List<String> NEWEST = List.of("kids-tee-b", "kids-tee-a", "men-shirt", "men-socks", "women-dress", "men-coat");
    private static final List<String> BY_NAME = List.of("men-socks", "kids-tee-b", "kids-tee-a", "men-shirt", "women-dress", "men-coat");
    private static final List<String> PRICE_ASC = List.of("women-dress", "men-socks", "kids-tee-b", "kids-tee-a", "men-shirt", "men-coat");
    private static final List<String> PRICE_DESC = List.of("men-coat", "men-shirt", "kids-tee-b", "kids-tee-a", "men-socks", "women-dress");

    private static final Set<String> INTERNAL_FIELDS =
            Set.of("_id", "_class", "active", "featured", "categoryId", "createdAt", "updatedAt", "parentId", "basePrice");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());

        mongoTemplate.getCollection("categories").insertMany(List.of(
                category(MEN, "men", "Men", "Clothing for men.", 1, true, "https://img.example/men.jpg"),
                category(WOMEN, "women", "Women", null, 2, true, null),
                category(ACCESSORIES, "accessories", "Accessories", null, 1, true, null),
                category(ARCHIVE, "archive", "Archive", "Retired", 0, false, null)));
        mongoTemplate.insert(new Category(KIDS.toHexString(), "kids", "Kids", null, null, null, 2, true, BASE, BASE));

        mongoTemplate.getCollection("products").insertMany(List.of(
                product(SHIRT, "men-shirt", "Classic Shirt", MEN, "Acme", "49.90", "39.90", true, true, 2,
                        List.of(image("https://img.example/shirt-front.jpg", "Front"),
                                image("https://img.example/shirt-back.jpg", "Back"))),
                product(COAT, "men-coat", "Wool Coat", MEN, null, "189", null, true, false, 20, null),
                product(SOCKS, "men-socks", "Ankle Socks", MEN, null, "10.5", null, true, false, 3, List.of()),
                product(RETIRED, "men-retired", "Retired Tee", MEN, null, "5.00", null, false, true, 0, List.of()),
                product(DRESS, "women-dress", "Summer Dress", WOMEN, null, "9.99", null, true, true, 5,
                        List.of(image("https://img.example/dress.jpg", "Dress")))));
        mongoTemplate.insert(new Product(TEE_A.toHexString(), "kids-tee-a", "Basic Tee", "Tee", KIDS.toHexString(), null,
                List.of(new ProductImage("https://img.example/tee-a.jpg", "Tee A")), new BigDecimal("25.00"), null,
                true, false, daysAgo(1), daysAgo(1)));
        mongoTemplate.getCollection("products").insertOne(
                product(TEE_B, "kids-tee-b", "Basic Tee", KIDS, null, "25.00", null, true, false, 1, List.of()));
    }

    private static Instant daysAgo(int days) {
        return BASE.minus(Duration.ofDays(days));
    }

    private static Document image(String url, String alt) {
        return new Document("url", url).append("alt", alt);
    }

    private static Document category(ObjectId id, String slug, String name, String description, int order,
            boolean active, String imageUrl) {
        Date created = Date.from(BASE);
        return new Document("_id", id).append("slug", slug).append("name", name).append("description", description)
                .append("parentId", null).append("imageUrl", imageUrl).append("displayOrder", order)
                .append("active", active).append("createdAt", created).append("updatedAt", created);
    }

    private static Document product(ObjectId id, String slug, String name, ObjectId categoryId, String brand,
            String basePrice, String salePrice, boolean active, boolean featured, int ageDays, List<Document> images) {
        Date created = Date.from(daysAgo(ageDays));
        Document doc = new Document("_id", id).append("slug", slug).append("name", name)
                .append("description", "Description of " + name).append("categoryId", categoryId).append("brand", brand)
                .append("basePrice", Decimal128.parse(basePrice))
                .append("salePrice", salePrice == null ? null : Decimal128.parse(salePrice))
                .append("active", active).append("featured", featured)
                .append("createdAt", created).append("updatedAt", created);
        if (images != null) {
            doc.append("images", images);
        }
        return doc;
    }

    record Response(int status, java.net.http.HttpHeaders headers, String body) {

        DocumentContext json() {
            return JsonPath.parse(body);
        }

        <T> T read(String path) {
            return JsonPath.read(body, path);
        }

        String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        String contentType() {
            return header("Content-Type");
        }
    }

    private Response send(String method, String pathAndQuery, String... headerPairs) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
                .timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.noBody());
        for (int i = 0; i < headerPairs.length; i += 2) {
            builder.header(headerPairs[i], headerPairs[i + 1]);
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.headers(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Response get(String pathAndQuery, String... headerPairs) {
        return send("GET", pathAndQuery, headerPairs);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private List<String> slugs(Response response) {
        return response.read("$.items[*].slug");
    }

    private void assertProblem(Response response, int status, String code) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.<Integer>read("$.status")).isEqualTo(status);
        assertThat(response.<String>read("$.code")).isEqualTo(code);
    }

    @SuppressWarnings("unchecked")
    private static void collectKeys(Object node, Set<String> into) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                into.add((String) k);
                collectKeys(v, into);
            });
        } else if (node instanceof List<?> list) {
            list.forEach(item -> collectKeys(item, into));
        }
    }

    private static Set<String> allKeys(Response response) {
        Set<String> keys = new java.util.HashSet<>();
        collectKeys(JsonPath.parse(response.body()).read("$"), keys);
        return keys;
    }

    @Nested
    @DisplayName("GET /api/v1/categories")
    class Categories {

        @Test
        void listsOnlyActiveCategoriesSortedByDisplayOrderThenName() {
            Response response = get("/api/v1/categories");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.contentType()).startsWith("application/json");
            assertThat(response.<List<String>>read("$[*].slug")).containsExactly("accessories", "men", "kids", "women");
        }

        @Test
        void categoryHasExactContractShapeWithExplicitNulls() {
            Response response = get("/api/v1/categories");

            Map<String, Object> men = response.<List<Map<String, Object>>>read("$[?(@.slug == 'men')]").getFirst();
            assertThat(men).containsOnlyKeys("id", "slug", "name", "description", "imageUrl", "displayOrder");
            assertThat(men).containsEntry("id", MEN.toHexString())
                    .containsEntry("name", "Men")
                    .containsEntry("description", "Clothing for men.")
                    .containsEntry("imageUrl", "https://img.example/men.jpg")
                    .containsEntry("displayOrder", 1);

            Map<String, Object> women = response.<List<Map<String, Object>>>read("$[?(@.slug == 'women')]").getFirst();
            assertThat(women).containsOnlyKeys("id", "slug", "name", "description", "imageUrl", "displayOrder");
            assertThat(women.get("description")).isNull();
            assertThat(women.get("imageUrl")).isNull();
        }

        @Test
        void categoryStoredWithClassHintIsMappedAndHintNotExposed() {
            Response response = get("/api/v1/categories/kids");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<String>read("$.id")).isEqualTo(KIDS.toHexString());
            assertThat(allKeys(response)).doesNotContainAnyElementsOf(INTERNAL_FIELDS);
        }

        @Test
        void getsActiveCategoryBySlug() {
            Response response = get("/api/v1/categories/men");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<String>read("$.slug")).isEqualTo("men");
            assertThat(response.<String>read("$.id")).isEqualTo(MEN.toHexString());
        }

        @ParameterizedTest(name = "slug={0}")
        @ValueSource(strings = {"archive", "does-not-exist", "MEN"})
        void inactiveUnknownOrDifferentlyCasedCategoryReturns404(String slug) {
            assertProblem(get("/api/v1/categories/" + slug), 404, "CATEGORY_NOT_FOUND");
        }

        @ParameterizedTest(name = "slug={0}")
        @ValueSource(strings = {".*", "^m", "m.n", "men|women", "{\"$ne\":null}", "{\"$regex\":\".*\"}", "[$ne]"})
        void slugWithRegexOrOperatorSyntaxIsTreatedLiterally(String slug) {
            assertProblem(get("/api/v1/categories/" + enc(slug)), 404, "CATEGORY_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class ProductList {

        @Test
        void defaultsReturnActiveProductsNewestFirstWithPageEnvelope() {
            Response response = get("/api/v1/products");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<Map<String, Object>>read("$")).containsOnlyKeys("items", "page", "size", "totalItems", "totalPages");
            assertThat(slugs(response)).containsExactlyElementsOf(NEWEST).doesNotContain("men-retired");
            assertThat(response.<Integer>read("$.page")).isZero();
            assertThat(response.<Integer>read("$.size")).isEqualTo(20);
            assertThat(response.<Integer>read("$.totalItems")).isEqualTo(6);
            assertThat(response.<Integer>read("$.totalPages")).isEqualTo(1);
        }

        @Test
        void cardHasExactContractShapeWithPrimaryImageAndSalePrice() {
            Response response = get("/api/v1/products?category=men&sort=name");

            Map<String, Object> shirt = response.<List<Map<String, Object>>>read("$.items[?(@.slug == 'men-shirt')]").getFirst();
            assertThat(shirt).containsOnlyKeys("id", "slug", "name", "primaryImage", "price", "salePrice");
            assertThat(shirt).containsEntry("id", SHIRT.toHexString()).containsEntry("name", "Classic Shirt");
            assertThat(shirt.get("primaryImage")).isEqualTo(Map.of("url", "https://img.example/shirt-front.jpg", "alt", "Front"));
            assertThat(shirt.get("price")).isEqualTo(Map.of("amount", "49.90", "currency", "ILS"));
            assertThat(shirt.get("salePrice")).isEqualTo(Map.of("amount", "39.90", "currency", "ILS"));
        }

        @Test
        void productWithoutImagesHasExplicitNullPrimaryImage() {
            Response response = get("/api/v1/products?category=men&sort=name");

            for (String slug : List.of("men-coat", "men-socks")) {
                Map<String, Object> card = response.<List<Map<String, Object>>>read("$.items[?(@.slug == '" + slug + "')]").getFirst();
                assertThat(card).containsKey("primaryImage");
                assertThat(card.get("primaryImage")).as(slug).isNull();
                assertThat(card).containsKey("salePrice");
                assertThat(card.get("salePrice")).as(slug).isNull();
            }
        }

        @Test
        void decimal128PricesRenderWithExactlyTwoFractionDigits() {
            Response response = get("/api/v1/products?sort=price_asc");

            assertThat(response.<List<String>>read("$.items[*].price.amount"))
                    .containsExactly("9.99", "10.50", "25.00", "25.00", "49.90", "189.00");
            assertThat(response.<List<String>>read("$.items[*].price.currency")).containsOnly("ILS");
        }

        @Test
        void filtersByActiveCategory() {
            Response response = get("/api/v1/products?category=men");

            assertThat(response.status()).isEqualTo(200);
            assertThat(slugs(response)).containsExactly("men-shirt", "men-socks", "men-coat");
            assertThat(response.<Integer>read("$.totalItems")).isEqualTo(3);
        }

        @Test
        void emptyCategoryReturnsEmptyPageNotAnError() {
            Response response = get("/api/v1/products?category=accessories");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<List<Object>>read("$.items")).isEmpty();
            assertThat(response.<Integer>read("$.page")).isZero();
            assertThat(response.<Integer>read("$.totalItems")).isZero();
            assertThat(response.<Integer>read("$.totalPages")).isZero();
        }

        @ParameterizedTest(name = "category={0}")
        @ValueSource(strings = {"archive", "nope", ".*", "{\"$ne\":null}", "{\"$gt\":\"\"}"})
        void inactiveUnknownOrInjectedCategoryReturns404(String category) {
            assertProblem(get("/api/v1/products?category=" + enc(category)), 404, "CATEGORY_NOT_FOUND");
        }

        @Test
        void featuredTrueReturnsOnlyActiveFeaturedProducts() {
            Response response = get("/api/v1/products?featured=true");

            assertThat(slugs(response)).containsExactly("men-shirt", "women-dress");
            assertThat(response.<Integer>read("$.totalItems")).isEqualTo(2);
        }

        @Test
        void featuredFalseReturnsOnlyActiveNonFeaturedProducts() {
            Response response = get("/api/v1/products?featured=false");

            assertThat(slugs(response)).containsExactly("kids-tee-b", "kids-tee-a", "men-socks", "men-coat");
        }

        @Test
        void featuredAndCategoryFiltersCombine() {
            Response response = get("/api/v1/products?category=men&featured=true");

            assertThat(slugs(response)).containsExactly("men-shirt");
            assertThat(response.<Integer>read("$.totalItems")).isEqualTo(1);
        }

        @ParameterizedTest(name = "sort={0}")
        @CsvSource({"newest", "name", "price_asc", "price_desc"})
        void everySortOrderEndToEnd(String sort) {
            Response response = get("/api/v1/products?sort=" + sort);

            assertThat(response.status()).isEqualTo(200);
            assertThat(slugs(response)).containsExactlyElementsOf(expectedOrder(sort));
        }

        @ParameterizedTest(name = "sort={0}, size={1}")
        @CsvSource({"newest, 1", "name, 1", "price_asc, 1", "price_desc, 1", "newest, 4", "price_desc, 4"})
        void walkingAllPagesYieldsEveryProductExactlyOnceInSortOrder(String sort, int size) {
            List<String> collected = new ArrayList<>();
            int expectedPages = (6 + size - 1) / size;
            for (int page = 0; page < expectedPages; page++) {
                Response response = get("/api/v1/products?sort=" + sort + "&size=" + size + "&page=" + page);
                assertThat(response.status()).isEqualTo(200);
                assertThat(response.<Integer>read("$.page")).isEqualTo(page);
                assertThat(response.<Integer>read("$.size")).isEqualTo(size);
                assertThat(response.<Integer>read("$.totalItems")).isEqualTo(6);
                assertThat(response.<Integer>read("$.totalPages")).isEqualTo(expectedPages);
                collected.addAll(slugs(response));
            }
            assertThat(collected).containsExactlyElementsOf(expectedOrder(sort));
        }

        @Test
        void tieBreakerOrdersEqualSortKeysByIdAndIsStableAcrossRepeatedCalls() {
            for (int i = 0; i < 3; i++) {
                assertThat(slugs(get("/api/v1/products?category=kids&sort=name&size=1&page=0"))).containsExactly("kids-tee-b");
                assertThat(slugs(get("/api/v1/products?category=kids&sort=name&size=1&page=1"))).containsExactly("kids-tee-a");
                assertThat(slugs(get("/api/v1/products?category=kids&sort=price_desc&size=1&page=0"))).containsExactly("kids-tee-b");
                assertThat(slugs(get("/api/v1/products?category=kids&sort=newest&size=1&page=1"))).containsExactly("kids-tee-a");
            }
        }

        @Test
        void lastPageIsPartial() {
            Response response = get("/api/v1/products?size=4&page=1");

            assertThat(response.status()).isEqualTo(200);
            assertThat(slugs(response)).containsExactly("women-dress", "men-coat");
            assertThat(response.<Integer>read("$.page")).isEqualTo(1);
            assertThat(response.<Integer>read("$.totalItems")).isEqualTo(6);
            assertThat(response.<Integer>read("$.totalPages")).isEqualTo(2);
        }

        @Test
        void pagePastTheEndReturnsEmptyItemsWithCorrectTotals() {
            Response response = get("/api/v1/products?size=4&page=5");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<List<Object>>read("$.items")).isEmpty();
            assertThat(response.<Integer>read("$.page")).isEqualTo(5);
            assertThat(response.<Integer>read("$.size")).isEqualTo(4);
            assertThat(response.<Integer>read("$.totalItems")).isEqualTo(6);
            assertThat(response.<Integer>read("$.totalPages")).isEqualTo(2);
        }

        @ParameterizedTest(name = "page={0}, size={1}")
        @CsvSource({"1073741824, 4", "2147483647, 2", "2147483647, 50"})
        void hugePageIndexDoesNotWrapAroundOrFail(String page, String size) {
            Response response = get("/api/v1/products?page=" + page + "&size=" + size);

            if (response.status() == 200) {
                assertThat(response.<List<Object>>read("$.items")).isEmpty();
                assertThat(response.<Integer>read("$.totalItems")).isEqualTo(6);
            } else {
                assertProblem(response, 400, "VALIDATION_ERROR");
            }
        }

        @ParameterizedTest(name = "size={0}")
        @ValueSource(ints = {1, 50})
        void sizeBoundsAreAccepted(int size) {
            Response response = get("/api/v1/products?size=" + size);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<Integer>read("$.size")).isEqualTo(size);
            assertThat(response.<List<Object>>read("$.items")).hasSize(Math.min(size, 6));
            assertThat(response.<Integer>read("$.totalPages")).isEqualTo((6 + size - 1) / size);
        }

        @ParameterizedTest(name = "{0}={1}")
        @CsvSource({
                "size, 0",
                "size, 51",
                "size, -5",
                "size, abc",
                "page, -1",
                "page, abc",
                "page, 1.5",
                "page, 99999999999",
                "sort, NEWEST",
                "sort, createdAt",
                "sort, '{\"$natural\":-1}'",
                "featured, maybe"})
        void invalidParameterReturns400ValidationErrorNamingTheField(String param, String value) {
            Response response = get("/api/v1/products?" + param + "=" + enc(value));

            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly(param);
            assertThat(response.<String>read("$.errors[0].message")).isNotBlank();
            assertThat(response.body()).doesNotContainIgnoringCase("exception").doesNotContain("java.");
        }

        @Test
        void configuredPaginationMaximumsAreAcceptedInclusive() {
            Response response = get("/api/v1/products?page=10000&size=50");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<Integer>read("$.page")).isEqualTo(10000);
            assertThat(response.<List<Object>>read("$.items")).isEmpty();
        }

        @ParameterizedTest(name = "{0}={1}")
        @CsvSource({"page, 10001, 10000", "size, 51, 50"})
        void valueAboveConfiguredMaximumReportsTheConfiguredLimit(String param, String value, String limit) {
            Response response = get("/api/v1/products?" + param + "=" + value);

            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(response.<List<Map<String, Object>>>read("$.errors")).containsExactly(
                    Map.of("field", param, "message", "must be less than or equal to " + limit));
        }

        @Test
        void maximumViolationsAreReportedTogetherWithOtherParameterErrors() {
            Response response = get("/api/v1/products?page=10001&size=51&sort=bogus");

            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly("page", "size", "sort");
        }

        @Test
        void multipleInvalidParametersAreAllReportedSortedByField() {
            Response response = get("/api/v1/products?page=-1&size=0&sort=bogus");

            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly("page", "size", "sort");
        }

        @Test
        void validationRunsBeforeCategoryLookup() {
            assertProblem(get("/api/v1/products?category=nope&page=-1"), 400, "VALIDATION_ERROR");
        }

        @Test
        void listResponseNeverContainsInternalFields() {
            Response response = get("/api/v1/products?size=50");

            assertThat(allKeys(response)).doesNotContainAnyElementsOf(INTERNAL_FIELDS);
            assertThat(response.body()).doesNotContain("\"_class\"").doesNotContain("com.clothingstore");
        }
    }

    private static List<String> expectedOrder(String sort) {
        return switch (sort) {
            case "newest" -> NEWEST;
            case "name" -> BY_NAME;
            case "price_asc" -> PRICE_ASC;
            case "price_desc" -> PRICE_DESC;
            default -> throw new IllegalArgumentException(sort);
        };
    }

    @Nested
    @DisplayName("GET /api/v1/products/{slug}")
    class ProductDetails {

        @Test
        void returnsFullDetailsWithAllImagesInOrderAndCategoryRef() {
            Response response = get("/api/v1/products/men-shirt");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<Map<String, Object>>read("$")).containsOnlyKeys(
                    "id", "slug", "name", "description", "brand", "images", "price", "salePrice", "category");
            assertThat(response.<String>read("$.id")).isEqualTo(SHIRT.toHexString());
            assertThat(response.<String>read("$.slug")).isEqualTo("men-shirt");
            assertThat(response.<String>read("$.name")).isEqualTo("Classic Shirt");
            assertThat(response.<String>read("$.description")).isEqualTo("Description of Classic Shirt");
            assertThat(response.<String>read("$.brand")).isEqualTo("Acme");
            assertThat(response.<List<Map<String, Object>>>read("$.images")).containsExactly(
                    Map.of("url", "https://img.example/shirt-front.jpg", "alt", "Front"),
                    Map.of("url", "https://img.example/shirt-back.jpg", "alt", "Back"));
            assertThat(response.<Map<String, Object>>read("$.price")).isEqualTo(Map.of("amount", "49.90", "currency", "ILS"));
            assertThat(response.<Map<String, Object>>read("$.salePrice")).isEqualTo(Map.of("amount", "39.90", "currency", "ILS"));
            assertThat(response.<Map<String, Object>>read("$.category")).isEqualTo(Map.of("slug", "men", "name", "Men"));
        }

        @Test
        void productWithoutImagesBrandOrSalePriceHasEmptyArrayAndNulls() {
            Response response = get("/api/v1/products/men-coat");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<List<Object>>read("$.images")).isEmpty();
            assertThat(response.<Map<String, Object>>read("$")).containsKeys("brand", "salePrice");
            assertThat(response.<Object>read("$.brand")).isNull();
            assertThat(response.<Object>read("$.salePrice")).isNull();
            assertThat(response.<String>read("$.price.amount")).isEqualTo("189.00");
        }

        @Test
        void cardSlugOpensDetailsForEveryListedProduct() {
            List<String> listed = slugs(get("/api/v1/products?size=50"));

            for (String slug : listed) {
                Response details = get("/api/v1/products/" + slug);
                assertThat(details.status()).as(slug).isEqualTo(200);
                assertThat(details.<String>read("$.slug")).isEqualTo(slug);
            }
        }

        @Test
        void productStoredWithClassHintIsMapped() {
            Response response = get("/api/v1/products/kids-tee-a");

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.<String>read("$.id")).isEqualTo(TEE_A.toHexString());
            assertThat(response.<String>read("$.price.amount")).isEqualTo("25.00");
            assertThat(response.<Map<String, Object>>read("$.category")).isEqualTo(Map.of("slug", "kids", "name", "Kids"));
        }

        @Test
        void detailsNeverContainInternalFields() {
            for (String slug : List.of("men-shirt", "kids-tee-a")) {
                Response response = get("/api/v1/products/" + slug);
                assertThat(allKeys(response)).as(slug).doesNotContainAnyElementsOf(INTERNAL_FIELDS);
            }
        }

        @ParameterizedTest(name = "slug={0}")
        @ValueSource(strings = {"men-retired", "does-not-exist", "MEN-SHIRT", "men-shirt "})
        void inactiveUnknownOrNonExactSlugReturns404(String slug) {
            assertProblem(get("/api/v1/products/" + enc(slug)), 404, "PRODUCT_NOT_FOUND");
        }

        @ParameterizedTest(name = "slug={0}")
        @ValueSource(strings = {".*", "^men", "men-.*", "men-shirt|.*", "{\"$ne\":null}", "{\"$regex\":\"men\"}",
                "{\"$gt\":\"\"}", "[$ne]"})
        void noSqlInjectionOrRegexSlugReturns404NotAMatch(String slug) {
            Response response = get("/api/v1/products/" + enc(slug));

            assertProblem(response, 404, "PRODUCT_NOT_FOUND");
            assertThat(response.body()).doesNotContain("men-shirt").doesNotContain("Classic Shirt");
        }

        @Test
        void problemDetailDoesNotLeakInternals() {
            Response response = get("/api/v1/products/does-not-exist");

            assertThat(response.body()).doesNotContain("Exception").doesNotContain("java.").doesNotContain("mongo");
        }
    }

    @Nested
    @DisplayName("CORS")
    class Cors {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/api/v1/categories", "/api/v1/products", "/api/v1/products/men-shirt", "/api/v1/categories/men"})
        void simpleGetFromAllowedOriginGetsAllowOriginHeader(String path) {
            Response response = get(path, "Origin", ALLOWED_ORIGIN);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            assertThat(response.header("Access-Control-Allow-Credentials")).isNull();
        }

        @Test
        void simpleGetFromDisallowedOriginIsRejectedWithoutAllowOrigin() {
            Response response = get("/api/v1/products", "Origin", DISALLOWED_ORIGIN);

            assertThat(response.status()).isEqualTo(403);
            assertThat(response.header("Access-Control-Allow-Origin")).isNull();
            assertThat(response.body()).doesNotContain("men-shirt");
        }

        @Test
        void localDevOriginFromDefaultProfileIsNotAllowedWhenOverridden() {
            assertThat(get("/api/v1/products", "Origin", "http://localhost:5173").status()).isEqualTo(403);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"GET", "HEAD"})
        void preflightFromAllowedOriginForReadMethodSucceeds(String method) {
            Response response = send("OPTIONS", "/api/v1/products",
                    "Origin", ALLOWED_ORIGIN, "Access-Control-Request-Method", method);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            assertThat(response.header("Access-Control-Allow-Methods")).contains("GET").doesNotContain("POST");
            assertThat(response.header("Access-Control-Max-Age")).isEqualTo("3600");
        }

        @Test
        void preflightFromDisallowedOriginIsRejected() {
            Response response = send("OPTIONS", "/api/v1/products",
                    "Origin", DISALLOWED_ORIGIN, "Access-Control-Request-Method", "GET");

            assertThat(response.status()).isEqualTo(403);
            assertThat(response.header("Access-Control-Allow-Origin")).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
        void preflightForWriteMethodIsRejectedEvenFromAllowedOrigin(String method) {
            Response response = send("OPTIONS", "/api/v1/products",
                    "Origin", ALLOWED_ORIGIN, "Access-Control-Request-Method", method);

            assertThat(response.status()).isEqualTo(403);
            assertThat(response.header("Access-Control-Allow-Origin")).isNull();
        }

        @Test
        void actualPostFromAllowedOriginIsNotAccepted() {
            Response response = send("POST", "/api/v1/products", "Origin", ALLOWED_ORIGIN);

            assertThat(response.status()).isIn(403, 405);
            assertThat(response.header("Access-Control-Allow-Origin")).isNull();
        }

        @Test
        void headRequestIsSupported() {
            Response response = send("HEAD", "/api/v1/products", "Origin", ALLOWED_ORIGIN);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            assertThat(response.body()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unknown routes and methods")
    class Routing {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/api/v1/nope", "/api/v2/products", "/api/v1/products/men-shirt/reviews"})
        void unknownRouteReturns404ProblemDetailWithoutInternals(String path) {
            Response response = get(path);

            assertThat(response.status()).isEqualTo(404);
            assertThat(response.contentType()).startsWith("application/problem+json");
            assertThat(response.<Integer>read("$.status")).isEqualTo(404);
            assertThat(response.body()).doesNotContain("Exception").doesNotContain("java.");
        }

        @Test
        void writeMethodOnCatalogResourceWithoutOriginIsMethodNotAllowed() {
            Response response = send("DELETE", "/api/v1/products/men-shirt");

            assertThat(response.status()).isEqualTo(405);
            assertThat(response.contentType()).startsWith("application/problem+json");
            assertThat(response.header("Allow")).contains("GET").doesNotContain("DELETE");
            assertThat(get("/api/v1/products/men-shirt").status()).isEqualTo(200);
        }
    }

    @Test
    void seedSanity() {
        assertThat(mongoTemplate.getCollection("products").countDocuments()).isEqualTo(7);
    }
}
