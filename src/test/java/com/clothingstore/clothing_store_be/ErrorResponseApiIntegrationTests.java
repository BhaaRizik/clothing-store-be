package com.clothingstore.clothing_store_be;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ErrorResponseApiIntegrationTests {

    private static final Date CREATED = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
    private static final ObjectId CATEGORY = new ObjectId("680000000000000000000a01");
    private static final ObjectId SHIRT = new ObjectId("680000000000000000000b01");
    private static final ObjectId ORPHAN_PRODUCT = new ObjectId("680000000000000000000b02");
    private static final ObjectId ORPHAN_CATEGORY = new ObjectId("680000000000000000000a99");
    private static final ObjectId SHIRT_BLUE_M = new ObjectId("680000000000000000000c01");
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String JSON = "application/json";
    private static final String CART_ID_HEADER = "X-Cart-Id";
    private static final String DEFAULT_TYPE = "about:blank";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void seed() {
        List.of("carts", "categories", "products", "product_variants")
                .forEach(collection -> mongoTemplate.getCollection(collection).deleteMany(new Document()));
        mongoTemplate.getCollection("categories").insertOne(new Document("_id", CATEGORY).append("slug", "men")
                .append("name", "Men").append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("products").insertMany(List.of(
                product(SHIRT, "oxford-shirt", CATEGORY),
                product(ORPHAN_PRODUCT, "orphan-shirt", ORPHAN_CATEGORY)));
        mongoTemplate.getCollection("product_variants").insertOne(new Document("_id", SHIRT_BLUE_M)
                .append("productId", SHIRT).append("sku", "ERR-BLU-M").append("size", "M")
                .append("color", new Document("name", "Blue").append("hexCode", "#0000FF"))
                .append("stockQuantity", 2).append("active", true).append("displayOrder", 1)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
    }

    private static Document product(ObjectId id, String slug, ObjectId categoryId) {
        return new Document("_id", id).append("slug", slug).append("name", "Oxford Shirt")
                .append("description", "Shirt").append("categoryId", categoryId)
                .append("basePrice", Decimal128.parse("50.00"))
                .append("images", List.of(new Document("url", "/img/blue.svg").append("alt", "Blue")
                        .append("color", "Blue")))
                .append("active", true).append("featured", false)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private CartApiTestResponse send(String method, String path, String body, String... headerPairs) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        for (int index = 0; index < headerPairs.length; index += 2) {
            builder.header(headerPairs[index], headerPairs[index + 1]);
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new CartApiTestResponse(response.statusCode(), response.headers(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private String createCartWithOneShirt() {
        String cartId = send("POST", "/api/v1/cart", null).read("$.id");
        CartApiTestResponse added = send("POST", "/api/v1/cart/items", addItemBody(SHIRT_BLUE_M, 1),
                "Content-Type", JSON, CART_ID_HEADER, cartId);
        assertThat(added.status()).isEqualTo(200);
        return cartId;
    }

    private static String addItemBody(ObjectId variantId, int quantity) {
        return "{\"variantId\":\"" + variantId.toHexString() + "\",\"quantity\":" + quantity + "}";
    }

    private static void assertProblem(CartApiTestResponse response, int status, String code, String title) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.header("Content-Type")).startsWith(PROBLEM_JSON);
        Map<String, Object> problem = response.read("$");
        assertThat(problem).containsKeys("type", "title", "status", "detail", "code");
        assertThat(problem.get("type")).isEqualTo(DEFAULT_TYPE);
        assertThat(problem.get("title")).isEqualTo(title);
        assertThat(problem.get("status")).isEqualTo(status);
        assertThat((String) problem.get("detail")).isNotBlank();
        assertThat(problem.get("code")).isEqualTo(code);
        assertThat(problem).doesNotContainKeys("trace", "exception", "message", "error", "timestamp");
    }

    @Test
    void unknownApiRouteReturnsRouteNotFoundProblemWithoutEchoingThePath() {
        CartApiTestResponse response = send("GET", "/api/v1/unknown-endpoint-7f3a", null);

        assertProblem(response, 404, "ROUTE_NOT_FOUND", "Route not found");
        assertThat(response.<String>read("$.detail")).isEqualTo("The requested endpoint does not exist.");
        assertThat(response.body()).doesNotContain("unknown-endpoint-7f3a");
    }

    @Test
    void unknownSubRouteOfExistingResourceReturnsRouteNotFound() {
        assertProblem(send("GET", "/api/v1/products/oxford-shirt/reviews", null), 404, "ROUTE_NOT_FOUND",
                "Route not found");
    }

    @Test
    void putOnProductsReturnsMethodNotAllowedProblemWithAllowHeader() {
        CartApiTestResponse response = send("PUT", "/api/v1/products", "{}", "Content-Type", JSON);

        assertProblem(response, 405, "METHOD_NOT_ALLOWED", "Method not allowed");
        assertThat(response.header("Allow")).contains("GET").doesNotContain("PUT");
        assertThat(response.<String>read("$.detail")).doesNotContain("PUT");
    }

    @Test
    void deleteOnPromotionsReturnsMethodNotAllowedProblem() {
        CartApiTestResponse response = send("DELETE", "/api/v1/promotions", null);

        assertProblem(response, 405, "METHOD_NOT_ALLOWED", "Method not allowed");
        assertThat(response.header("Allow")).contains("GET");
    }

    @Test
    void getOnCartItemsCollectionReturnsMethodNotAllowedProblemWithPostAllowed() {
        String cartId = send("POST", "/api/v1/cart", null).read("$.id");

        CartApiTestResponse response = send("GET", "/api/v1/cart/items", null, CART_ID_HEADER, cartId);

        assertProblem(response, 405, "METHOD_NOT_ALLOWED", "Method not allowed");
        assertThat(response.header("Allow")).contains("POST");
    }

    @Test
    void addItemWithTextPlainReturnsUnsupportedMediaTypeAndLeavesCartUnchanged() {
        String cartId = createCartWithOneShirt();
        Document before = mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first();

        CartApiTestResponse response = send("POST", "/api/v1/cart/items", addItemBody(SHIRT_BLUE_M, 1),
                "Content-Type", "text/plain", CART_ID_HEADER, cartId);

        assertProblem(response, 415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type");
        assertThat(response.body()).doesNotContain("text/plain");
        Document after = mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first();
        assertThat(after).isEqualTo(before);
        CartApiTestResponse cart = send("GET", "/api/v1/cart", null, CART_ID_HEADER, cartId);
        assertThat(cart.<Integer>read("$.itemCount")).isEqualTo(1);
    }

    @Test
    void updateQuantityWithXmlBodyReturnsUnsupportedMediaTypeAndLeavesCartUnchanged() {
        String cartId = createCartWithOneShirt();

        CartApiTestResponse response = send("PATCH", "/api/v1/cart/items/" + SHIRT_BLUE_M.toHexString(),
                "<quantity>2</quantity>", "Content-Type", "application/xml", CART_ID_HEADER, cartId);

        assertProblem(response, 415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type");
        CartApiTestResponse cart = send("GET", "/api/v1/cart", null, CART_ID_HEADER, cartId);
        assertThat(cart.<Integer>read("$.items[0].quantity")).isEqualTo(1);
    }

    @Test
    void xmlOnlyClientReceivesNotAcceptableAsProblemJson() {
        assertProblem(send("GET", "/api/v1/products", null, "Accept", "application/xml"), 406,
                "NOT_ACCEPTABLE", "Not acceptable");
    }

    @Test
    void missingCartIdHeaderReturnsValidationErrorNamingTheHeader() {
        CartApiTestResponse response = send("GET", "/api/v1/cart", null);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.<String>read("$.errors[0].field")).isEqualTo(CART_ID_HEADER);
    }

    @Test
    void missingCartIdHeaderOnAddItemReturnsValidationErrorAndCreatesNoCart() {
        CartApiTestResponse response = send("POST", "/api/v1/cart/items", addItemBody(SHIRT_BLUE_M, 1),
                "Content-Type", JSON);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(mongoTemplate.getCollection("carts").countDocuments()).isZero();
    }

    @Test
    void nonNumericPageQueryReturnsValidationErrorNamingTheParameter() {
        CartApiTestResponse response = send("GET", "/api/v1/products?page=abc", null);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.<String>read("$.errors[0].field")).isEqualTo("page");
    }

    @Test
    void outOfRangePaginationReturnsValidationError() {
        assertProblem(send("GET", "/api/v1/products?page=-1", null), 400, "VALIDATION_ERROR", "Validation failed");
        assertProblem(send("GET", "/api/v1/products?size=0", null), 400, "VALIDATION_ERROR", "Validation failed");
        assertProblem(send("GET", "/api/v1/products?sort=cheapest", null), 400, "VALIDATION_ERROR",
                "Validation failed");
    }

    @Test
    void malformedJsonBodyReturnsValidationErrorWithoutParserInternals() {
        String cartId = send("POST", "/api/v1/cart", null).read("$.id");

        CartApiTestResponse response = send("POST", "/api/v1/cart/items", "{\"variantId\":", "Content-Type", JSON,
                CART_ID_HEADER, cartId);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.body()).doesNotContain("Jackson", "tools.jackson", "Unexpected end-of-input", "line:");
    }

    @Test
    void unknownCategorySlugReturnsCategoryNotFoundProblem() {
        assertProblem(send("GET", "/api/v1/categories/unknown-slug", null), 404, "CATEGORY_NOT_FOUND",
                "Resource not found");
    }

    @Test
    void unknownCategoryFilterOnProductsReturnsCategoryNotFoundProblem() {
        assertProblem(send("GET", "/api/v1/products?category=unknown-slug", null), 404, "CATEGORY_NOT_FOUND",
                "Resource not found");
    }

    @Test
    void unknownProductSlugReturnsProductNotFoundProblem() {
        assertProblem(send("GET", "/api/v1/products/unknown-slug", null), 404, "PRODUCT_NOT_FOUND",
                "Resource not found");
    }

    @Test
    void unknownCartIdReturnsCartNotFoundProblem() {
        assertProblem(send("GET", "/api/v1/cart", null, CART_ID_HEADER, UUID.randomUUID().toString()), 404,
                "CART_NOT_FOUND", "Resource not found");
        assertProblem(send("GET", "/api/v1/cart", null, CART_ID_HEADER, "not-a-uuid"), 404,
                "CART_NOT_FOUND", "Resource not found");
    }

    @Test
    void removingAnItemNotInTheCartReturnsCartItemNotFoundProblem() {
        String cartId = send("POST", "/api/v1/cart", null).read("$.id");

        assertProblem(send("DELETE", "/api/v1/cart/items/" + SHIRT_BLUE_M.toHexString(), null,
                CART_ID_HEADER, cartId), 404, "CART_ITEM_NOT_FOUND", "Resource not found");
    }

    @Test
    void addingMoreThanStockReturnsInsufficientStockConflictProblemAndLeavesCartEmpty() {
        String cartId = send("POST", "/api/v1/cart", null).read("$.id");

        CartApiTestResponse response = send("POST", "/api/v1/cart/items", addItemBody(SHIRT_BLUE_M, 3),
                "Content-Type", JSON, CART_ID_HEADER, cartId);

        assertProblem(response, 409, "INSUFFICIENT_STOCK", "Conflict");
        assertThat(send("GET", "/api/v1/cart", null, CART_ID_HEADER, cartId).<Integer>read("$.itemCount"))
                .isZero();
    }

    @Test
    void domainNotFoundIsStillProblemJsonWhenClientOnlyAcceptsXml() {
        assertProblem(send("GET", "/api/v1/products/unknown-slug", null, "Accept", "application/xml"), 404,
                "PRODUCT_NOT_FOUND", "Resource not found");
    }

    @Test
    void trailingSlashAndNonApiPathsReturnRouteNotFoundProblem() {
        assertProblem(send("GET", "/api/v1/products/", null), 404, "ROUTE_NOT_FOUND", "Route not found");
        assertProblem(send("GET", "/not-an-api-path", null), 404, "ROUTE_NOT_FOUND", "Route not found");
    }

    @Test
    void headOnUnknownRouteReturns404ProblemContentTypeWithoutBody() {
        CartApiTestResponse response = send("HEAD", "/api/v1/unknown-endpoint-7f3a", null);

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.header("Content-Type")).startsWith(PROBLEM_JSON);
        assertThat(response.body()).isEmpty();
    }

    @Test
    void overflowingPageQueryReturnsValidationErrorInsteadOfServerError() {
        CartApiTestResponse response = send("GET", "/api/v1/products?page=99999999999", null);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.<String>read("$.errors[0].field")).isEqualTo("page");
    }

    @Test
    void invalidFeaturedFlagReturnsValidationErrorNamingTheParameter() {
        CartApiTestResponse response = send("GET", "/api/v1/products?featured=maybe", null);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.<String>read("$.errors[0].field")).isEqualTo("featured");
    }

    @Test
    void missingRequestBodyReturnsValidationErrorAndCreatesNoCartItem() {
        String cartId = send("POST", "/api/v1/cart", null).read("$.id");

        CartApiTestResponse response = send("POST", "/api/v1/cart/items", null, "Content-Type", JSON,
                CART_ID_HEADER, cartId);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(send("GET", "/api/v1/cart", null, CART_ID_HEADER, cartId).<Integer>read("$.itemCount"))
                .isZero();
    }

    @Test
    void unexpectedServerFailureReturnsInternalErrorWithoutLeakingInternals() {
        CartApiTestResponse response = send("GET", "/api/v1/products/orphan-shirt", null);

        assertProblem(response, 500, "INTERNAL_ERROR", "Internal server error");
        assertThat(response.<String>read("$.detail"))
                .isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.body()).doesNotContain("Data integrity", ORPHAN_PRODUCT.toHexString(),
                "IllegalStateException", "java.", "at com.clothingstore");
    }

    @Test
    void containerRejectedPathReturnsProblemJson() {
        CartApiTestResponse response = send("GET", "/api/v1/products/%2F", null);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.body()).doesNotContain("<html", "Tomcat", "%2F");
    }

    @Test
    void containerRejectedNullBytePathReturnsProblemJson() {
        CartApiTestResponse response = send("GET", "/api/v1/products/a%00b", null);

        assertProblem(response, 400, "VALIDATION_ERROR", "Validation failed");
        assertThat(response.body()).doesNotContain("<html", "Tomcat");
    }

    @Test
    void errorFallbackEndpointReturnsProblemJson() {
        CartApiTestResponse response = send("GET", "/error", null);

        assertProblem(response, 500, "INTERNAL_ERROR", "Internal server error");
        assertThat(response.body()).doesNotContain("timestamp", "999");
    }

    @Test
    void errorFallbackEndpointReturnsProblemJsonForHtmlClients() {
        CartApiTestResponse response = send("GET", "/error", null, "Accept", "text/html");

        assertProblem(response, 500, "INTERNAL_ERROR", "Internal server error");
    }
}
