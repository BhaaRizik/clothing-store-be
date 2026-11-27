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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.cors.allowed-origins=" + CartApiIntegrationTests.ALLOWED_ORIGIN,
        "app.cart.max-item-quantity=10",
        "app.cart.inactive-cart-expiry=30d"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CartRequestValidationApiIntegrationTests {

    private static final Date CREATED = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
    private static final ObjectId CATEGORY = new ObjectId("680000000000000000000a01");
    private static final ObjectId JACKET = new ObjectId("680000000000000000000b01");
    private static final ObjectId JACKET_M = new ObjectId("680000000000000000000c01");
    private static final ObjectId JACKET_L = new ObjectId("680000000000000000000c02");
    private static final ObjectId JACKET_SOLD_OUT = new ObjectId("680000000000000000000c03");
    private static final String ITEMS_PATH = "/api/v1/cart/items";

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection("carts").deleteMany(new Document());
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());
        mongoTemplate.getCollection("product_variants").deleteMany(new Document());
        mongoTemplate.getCollection("categories").insertOne(new Document("_id", CATEGORY).append("slug", "outerwear")
                .append("name", "Outerwear").append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("products").insertOne(new Document("_id", JACKET).append("slug", "rain-jacket")
                .append("name", "Rain Jacket").append("description", "Jacket").append("categoryId", CATEGORY)
                .append("basePrice", Decimal128.parse("120.00")).append("images", List.of())
                .append("active", true).append("featured", false)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("product_variants").insertMany(List.of(
                variant(JACKET_M, "JACKET-M", "M", 50),
                variant(JACKET_L, "JACKET-L", "L", 3),
                variant(JACKET_SOLD_OUT, "JACKET-XL", "XL", 0)));
    }

    private static Document variant(ObjectId id, String sku, String size, int stock) {
        return new Document("_id", id).append("productId", JACKET).append("sku", sku).append("size", size)
                .append("color", new Document("name", "Black").append("hexCode", "#000000"))
                .append("stockQuantity", stock).append("active", true).append("displayOrder", 1)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private CartApiTestClient cartApi() {
        return new CartApiTestClient(port);
    }

    private Document storedCart(String cartId) {
        return mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first();
    }

    private static String addBody(ObjectId variantId, String quantity) {
        return "{\"variantId\":\"" + variantId.toHexString() + "\",\"quantity\":" + quantity + "}";
    }

    private static void assertProblem(CartApiTestResponse response, int status, String code) {
        assertThat(response.status()).as(response.body()).isEqualTo(status);
        assertThat(response.header("Content-Type")).startsWith("application/problem+json");
        assertThat(response.<String>read("$.code")).isEqualTo(code);
        assertThat(response.body()).doesNotContain("Exception").doesNotContain("java.").doesNotContain("jackson");
    }

    @Test
    void rejectedAddsOnFreshCartCreateNoItemAndKeepUpdatedAt() {
        String cartId = cartApi().createCart();
        Document before = storedCart(cartId);
        List<String> rejectedBodies = List.of(
                "{\"variantId\":\"not-an-id\",\"quantity\":1}",
                addBody(JACKET_M, "0"),
                addBody(JACKET_M, "11"),
                addBody(JACKET_M, "2147483648"),
                "{}",
                "",
                "[]");

        for (String body : rejectedBodies) {
            assertProblem(cartApi().addItemWithBody(cartId, body), 400, "VALIDATION_ERROR");
            assertThat(storedCart(cartId)).as(body).isEqualTo(before);
        }
        assertProblem(cartApi().addItemWithBody(cartId, addBody(JACKET_SOLD_OUT, "1")), 409, "VARIANT_UNAVAILABLE");
        assertProblem(cartApi().addItemWithBody(cartId, addBody(new ObjectId(), "1")), 409, "VARIANT_UNAVAILABLE");
        assertProblem(cartApi().addItemWithBody(cartId, addBody(JACKET_L, "4")), 409, "INSUFFICIENT_STOCK");

        Document after = storedCart(cartId);
        assertThat(after).isEqualTo(before);
        assertThat(after.getList("items", Document.class)).isEmpty();
        assertThat(after.get("updatedAt")).isEqualTo(before.get("updatedAt"));
        assertThat(cartApi().getCart(cartId).<Integer>read("$.itemCount")).isZero();
    }

    @Test
    void stockAndLimitRejectionsOnExistingLineLeaveStoredCartUnchanged() {
        String cartId = cartApi().createCart();
        assertThat(cartApi().addItem(cartId, JACKET_M.toHexString(), 8).status()).isEqualTo(200);
        assertThat(cartApi().addItem(cartId, JACKET_L.toHexString(), 2).status()).isEqualTo(200);
        Document before = storedCart(cartId);

        CartApiTestResponse limit = cartApi().addItem(cartId, JACKET_M.toHexString(), 3);
        CartApiTestResponse stockOnAdd = cartApi().addItem(cartId, JACKET_L.toHexString(), 2);
        CartApiTestResponse stockOnPatch = cartApi().updateQuantity(cartId, JACKET_L.toHexString(), 4);

        assertProblem(limit, 409, "CART_ITEM_LIMIT_EXCEEDED");
        assertProblem(stockOnAdd, 409, "INSUFFICIENT_STOCK");
        assertProblem(stockOnPatch, 409, "INSUFFICIENT_STOCK");
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @ParameterizedTest(name = "quantity={0}")
    @ValueSource(strings = {"2147483648", "-2147483649", "99999999999999999999", "9223372036854775808", "1e10",
            "1E400"})
    void hugeOrOverflowingQuantityIsRejectedOnQuantityFieldNotAsServerError(String quantity) {
        String cartId = cartApi().createCart();
        assertThat(cartApi().addItem(cartId, JACKET_M.toHexString(), 1).status()).isEqualTo(200);
        Document before = storedCart(cartId);

        CartApiTestResponse added = cartApi().addItemWithBody(cartId, addBody(JACKET_L, quantity));
        CartApiTestResponse patched = cartApi().updateQuantityWithBody(cartId, JACKET_M.toHexString(),
                "{\"quantity\":" + quantity + "}");

        assertProblem(added, 400, "VALIDATION_ERROR");
        assertProblem(patched, 400, "VALIDATION_ERROR");
        assertThat(added.<List<String>>read("$.errors[*].field")).containsExactly("quantity");
        assertThat(patched.<List<String>>read("$.errors[*].field")).containsExactly("quantity");
        assertThat(added.body()).doesNotContain(quantity);
        assertThat(patched.body()).doesNotContain(quantity);
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @Test
    void emptyObjectBodyListsBothMissingFields() {
        String cartId = cartApi().createCart();

        CartApiTestResponse empty = cartApi().addItemWithBody(cartId, "{}");
        CartApiTestResponse nulls = cartApi().addItemWithBody(cartId, "{\"variantId\":null,\"quantity\":null}");

        for (CartApiTestResponse response : List.of(empty, nulls)) {
            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly("quantity", "variantId");
            assertThat(response.<List<String>>read("$.errors[*].message")).allSatisfy(message -> assertThat(message).isNotBlank());
        }
    }

    @Test
    void blankAndPaddedVariantIdsAreReportedOnVariantIdFieldTogetherWithQuantity() {
        String cartId = cartApi().createCart();
        for (String variantId : List.of("", "   ", " " + JACKET_M.toHexString(), JACKET_M.toHexString() + " ")) {
            CartApiTestResponse response = cartApi().addItemWithBody(cartId,
                    "{\"variantId\":\"" + variantId + "\",\"quantity\":0}");

            assertProblem(response, 400, "VALIDATION_ERROR");
            assertThat(response.<List<String>>read("$.errors[*].field")).as("'" + variantId + "'")
                    .containsExactly("quantity", "variantId");
        }
        assertThat(storedCart(cartId).getList("items", Document.class)).isEmpty();
    }

    @ParameterizedTest(name = "body={0}")
    @ValueSource(strings = {"null", "[]", "[{\"variantId\":\"680000000000000000000c01\",\"quantity\":1}]", "\"text\"",
            "42", "{\"variantId\":\"680000000000000000000c01\",\"quantity\":1", "{'variantId':'x'}"})
    void nonObjectOrMalformedJsonBodyIsRejectedWithoutWriting(String body) {
        String cartId = cartApi().createCart();
        Document before = storedCart(cartId);

        assertProblem(cartApi().addItemWithBody(cartId, body), 400, "VALIDATION_ERROR");
        assertProblem(cartApi().updateQuantityWithBody(cartId, JACKET_M.toHexString(), body), 400, "VALIDATION_ERROR");
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @Test
    void nonJsonContentTypeIsRejectedWithoutWriting() throws Exception {
        String cartId = cartApi().createCart();
        Document before = storedCart(cartId);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ITEMS_PATH))
                .timeout(Duration.ofSeconds(10))
                .header("X-Cart-Id", cartId)
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(addBody(JACKET_M, "1")))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @Test
    void unknownCartTakesPrecedenceOverMalformedPathVariantId() {
        String unknownCart = UUID.randomUUID().toString();

        assertProblem(cartApi().updateQuantity(unknownCart, "not-an-id", 1), 404, "CART_NOT_FOUND");
        assertProblem(cartApi().removeItem(unknownCart, "not-an-id"), 404, "CART_NOT_FOUND");
        assertProblem(cartApi().updateQuantity("not-a-uuid", "not-an-id", 1), 404, "CART_NOT_FOUND");
        assertProblem(cartApi().removeItem("not-a-uuid", "not-an-id"), 404, "CART_NOT_FOUND");
        assertThat(mongoTemplate.getCollection("carts").countDocuments()).isZero();
    }

    @Test
    void invalidPatchBodyTakesPrecedenceOverMalformedPathVariantIdAndUnknownCart() {
        CartApiTestResponse response = cartApi().updateQuantityWithBody(UUID.randomUUID().toString(), "not-an-id",
                "{\"quantity\":0}");

        assertProblem(response, 400, "VALIDATION_ERROR");
        assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly("quantity");
    }

    @Test
    void invalidAddBodyIsReportedBeforeUnknownCart() {
        CartApiTestResponse response = cartApi().addItemWithBody(UUID.randomUUID().toString(),
                "{\"variantId\":\"bad\",\"quantity\":0}");

        assertProblem(response, 400, "VALIDATION_ERROR");
        assertThat(response.<List<String>>read("$.errors[*].field")).containsExactly("quantity", "variantId");
        assertThat(mongoTemplate.getCollection("carts").countDocuments()).isZero();
    }

    @Test
    void blankCartIdHeaderIsTreatedAsUnknownCartWithoutCreatingOne() {
        CartApiTestResponse response = cartApi().sendWithHeaders("POST", ITEMS_PATH, addBody(JACKET_M, "1"),
                "X-Cart-Id", " ");

        assertThat(response.status()).isIn(400, 404);
        assertThat(response.<String>read("$.code")).isIn("VALIDATION_ERROR", "CART_NOT_FOUND");
        assertThat(mongoTemplate.getCollection("carts").countDocuments()).isZero();
    }

    @Test
    void uppercaseHexVariantIdAddsTheSameLineAsLowercase() {
        String cartId = cartApi().createCart();

        CartApiTestResponse upper = cartApi().addItem(cartId, JACKET_M.toHexString().toUpperCase(), 1);
        CartApiTestResponse lower = cartApi().addItem(cartId, JACKET_M.toHexString(), 1);

        assertThat(upper.status()).isEqualTo(200);
        assertThat(lower.<List<String>>read("$.items[*].variantId")).containsExactly(JACKET_M.toHexString());
        assertThat(lower.<Integer>read("$.items[0].quantity")).isEqualTo(2);
    }

    @Test
    void uppercaseHexPathVariantIdAddressesTheSameLineAsTheBody() {
        String cartId = cartApi().createCart();
        cartApi().addItem(cartId, JACKET_M.toHexString().toUpperCase(), 1);

        CartApiTestResponse patched = cartApi().updateQuantity(cartId, JACKET_M.toHexString().toUpperCase(), 2);
        CartApiTestResponse removed = cartApi().removeItem(cartId, JACKET_M.toHexString().toUpperCase());

        assertThat(patched.status()).as(patched.body()).isEqualTo(200);
        assertThat(patched.<Integer>read("$.items.length()")).isEqualTo(1);
        assertThat(patched.<Integer>read("$.items[0].quantity")).isEqualTo(2);
        assertThat(patched.<String>read("$.items[0].variantId")).isEqualTo(JACKET_M.toHexString());
        assertThat(removed.status()).as(removed.body()).isEqualTo(200);
        assertThat(storedCart(cartId).getList("items", Document.class)).isEmpty();
    }

    @Test
    void unknownQueryParametersAndClientOwnedFieldsDoNotChangeServerComputedCart() {
        String cartId = cartApi().createCart();

        CartApiTestResponse added = cartApi().sendWithHeaders("POST", ITEMS_PATH + "?price=0.01&subtotal=0",
                "{\"variantId\":\"" + JACKET_M.toHexString() + "\",\"quantity\":2,\"id\":\"" + UUID.randomUUID()
                        + "\",\"maxItemQuantity\":999,\"itemCount\":99,\"status\":\"AVAILABLE\",\"items\":[]}",
                "X-Cart-Id", cartId);

        assertThat(added.status()).isEqualTo(200);
        assertThat(added.<String>read("$.id")).isEqualTo(cartId);
        assertThat(added.<Integer>read("$.maxItemQuantity")).isEqualTo(10);
        assertThat(added.<Integer>read("$.itemCount")).isEqualTo(2);
        assertThat(added.<String>read("$.subtotal.amount")).isEqualTo("240.00");
        assertThat(storedCart(cartId).getList("items", Document.class)).hasSize(1);
        assertThat(mongoTemplate.getCollection("carts").countDocuments()).isEqualTo(1);
    }

    @Test
    void validationMessagesDoNotEchoMaliciousInput() {
        String cartId = cartApi().createCart();
        String payload = "<script>alert(1)</script>";

        CartApiTestResponse body = cartApi().addItemWithBody(cartId,
                "{\"variantId\":\"" + payload + "\",\"quantity\":\"" + payload + "\"}");
        CartApiTestResponse path = cartApi().updateQuantity(cartId, "%3Cscript%3E", 1);

        assertProblem(body, 400, "VALIDATION_ERROR");
        assertThat(body.body()).doesNotContain("script").doesNotContain("alert");
        assertProblem(path, 404, "CART_ITEM_NOT_FOUND");
        assertThat(path.body()).doesNotContain("script");
        assertThat(body.<List<Map<String, Object>>>read("$.errors")).isNotEmpty();
    }
}
