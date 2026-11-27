package com.clothingstore.clothing_store_be;

import com.clothingstore.clothing_store_be.model.cart.Cart;
import com.clothingstore.clothing_store_be.repository.cart.CartRepository;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.cors.allowed-origins=" + CartApiIntegrationTests.ALLOWED_ORIGIN,
        "app.cart.max-item-quantity=10",
        "app.cart.inactive-cart-expiry=30d"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CartApiIntegrationTests {

    static final String ALLOWED_ORIGIN = "https://shop.example.test";

    private static final Date CREATED = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
    private static final ObjectId CATEGORY = new ObjectId("670000000000000000000a01");
    private static final ObjectId SHIRT = new ObjectId("670000000000000000000b01");
    private static final ObjectId SHIRT_BLUE_M = new ObjectId("670000000000000000000c01");
    private static final ObjectId SHIRT_WHITE_L = new ObjectId("670000000000000000000c02");
    private static final ObjectId SHIRT_SOLD_OUT = new ObjectId("670000000000000000000c03");

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CartRepository cartRepository;

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection("carts").deleteMany(new Document());
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());
        mongoTemplate.getCollection("product_variants").deleteMany(new Document());
        mongoTemplate.getCollection("categories").insertOne(new Document("_id", CATEGORY).append("slug", "men")
                .append("name", "Men").append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("products").insertOne(new Document("_id", SHIRT).append("slug", "oxford-shirt")
                .append("name", "Oxford Shirt").append("description", "Shirt").append("categoryId", CATEGORY)
                .append("basePrice", Decimal128.parse("50.00")).append("salePrice", Decimal128.parse("40.00"))
                .append("images", List.of(
                        new Document("url", "/img/white.svg").append("alt", "White").append("color", "White"),
                        new Document("url", "/img/blue.svg").append("alt", "Blue").append("color", "Blue")))
                .append("active", true).append("featured", false)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("product_variants").insertMany(List.of(
                variant(SHIRT_BLUE_M, "SHIRT-BLU-M", "M", "Blue", "#0000FF", 6),
                variant(SHIRT_WHITE_L, "SHIRT-WHT-L", "L", "White", "#FFFFFF", 3),
                variant(SHIRT_SOLD_OUT, "SHIRT-WHT-XL", "XL", "White", "#FFFFFF", 0)));
    }

    private static Document variant(ObjectId id, String sku, String size, String color, String hex, int stock) {
        return new Document("_id", id).append("productId", SHIRT).append("sku", sku).append("size", size)
                .append("color", new Document("name", color).append("hexCode", hex))
                .append("stockQuantity", stock).append("active", true).append("displayOrder", 1)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private CartApiTestClient cartApi() {
        return new CartApiTestClient(port);
    }

    private CartApiTestResponse send(String method, String path, String body, String... headerPairs) {
        return cartApi().sendWithHeaders(method, path, body, headerPairs);
    }

    private String createCart() {
        CartApiTestResponse response = send("POST", "/api/v1/cart", null);
        assertThat(response.status()).isEqualTo(201);
        return response.read("$.id");
    }

    private CartApiTestResponse addItem(String cartId, ObjectId variantId, int quantity) {
        return cartApi().addItemWithBody(cartId,
                "{\"variantId\":\"" + variantId.toHexString() + "\",\"quantity\":" + quantity + ",\"price\":\"0.01\"}");
    }

    private static void assertProblem(CartApiTestResponse response, int status, String code) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.header("Content-Type")).startsWith("application/problem+json");
        assertThat(response.<String>read("$.code")).isEqualTo(code);
    }

    @Test
    void fullGuestCartFlow() {
        CartApiTestResponse created = send("POST", "/api/v1/cart", null);
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/api/v1/cart");
        String cartId = created.read("$.id");
        assertThat(UUID.fromString(cartId).toString()).isEqualTo(cartId);
        assertThat(created.<List<Object>>read("$.items")).isEmpty();
        assertThat(created.<String>read("$.subtotal.amount")).isEqualTo("0.00");
        assertThat(created.<Integer>read("$.maxItemQuantity")).isEqualTo(10);

        CartApiTestResponse added = addItem(cartId, SHIRT_BLUE_M, 2);
        assertThat(added.status()).isEqualTo(200);
        assertThat(added.<String>read("$.items[0].variantId")).isEqualTo(SHIRT_BLUE_M.toHexString());
        assertThat(added.<String>read("$.items[0].productSlug")).isEqualTo("oxford-shirt");
        assertThat(added.<String>read("$.items[0].image.url")).isEqualTo("/img/blue.svg");
        assertThat(added.<String>read("$.items[0].unitPrice.amount")).isEqualTo("40.00");
        assertThat(added.<String>read("$.items[0].originalPrice.amount")).isEqualTo("50.00");
        assertThat(added.<String>read("$.items[0].lineTotal.amount")).isEqualTo("80.00");
        assertThat(added.<String>read("$.items[0].status")).isEqualTo("AVAILABLE");

        addItem(cartId, SHIRT_WHITE_L, 1);
        CartApiTestResponse merged = addItem(cartId, SHIRT_BLUE_M, 3);
        assertThat(merged.status()).isEqualTo(200);
        assertThat(merged.<List<String>>read("$.items[*].variantId"))
                .containsExactly(SHIRT_BLUE_M.toHexString(), SHIRT_WHITE_L.toHexString());
        assertThat(merged.<Integer>read("$.items[0].quantity")).isEqualTo(5);
        assertThat(merged.<Integer>read("$.itemCount")).isEqualTo(6);
        assertThat(merged.<String>read("$.subtotal.amount")).isEqualTo("240.00");

        assertProblem(addItem(cartId, SHIRT_BLUE_M, 2), 409, "INSUFFICIENT_STOCK");
        assertProblem(addItem(cartId, SHIRT_SOLD_OUT, 1), 409, "VARIANT_UNAVAILABLE");

        CartApiTestResponse patched = send("PATCH", "/api/v1/cart/items/" + SHIRT_BLUE_M.toHexString(), "{\"quantity\":1}",
                "X-Cart-Id", cartId);
        assertThat(patched.status()).isEqualTo(200);
        assertThat(patched.<Integer>read("$.items[0].quantity")).isEqualTo(1);
        assertThat(patched.<String>read("$.subtotal.amount")).isEqualTo("80.00");

        CartApiTestResponse deleted = send("DELETE", "/api/v1/cart/items/" + SHIRT_WHITE_L.toHexString(), null, "X-Cart-Id", cartId);
        assertThat(deleted.status()).isEqualTo(200);
        assertThat(deleted.<List<String>>read("$.items[*].variantId")).containsExactly(SHIRT_BLUE_M.toHexString());

        CartApiTestResponse fetched = send("GET", "/api/v1/cart", null, "X-Cart-Id", cartId);
        assertThat(fetched.status()).isEqualTo(200);
        assertThat(fetched.header("Cache-Control")).isEqualTo("no-store");
        assertThat(fetched.<Integer>read("$.itemCount")).isEqualTo(1);

        assertProblem(send("DELETE", "/api/v1/cart/items/" + SHIRT_WHITE_L.toHexString(), null, "X-Cart-Id", cartId),
                404, "CART_ITEM_NOT_FOUND");
    }

    @Test
    void storedCartUsesTypedFieldsAndVersion() {
        String cartId = createCart();
        addItem(cartId, SHIRT_BLUE_M, 2);

        Document stored = mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first();

        assertThat(stored).isNotNull();
        assertThat(stored.get("version")).isEqualTo(1L);
        assertThat(stored.get("updatedAt")).isInstanceOf(Date.class);
        Document item = stored.getList("items", Document.class).getFirst();
        assertThat(item.get("variantId")).isEqualTo(SHIRT_BLUE_M);
        assertThat(item.get("productId")).isEqualTo(SHIRT);
        assertThat(item.get("unitPriceSnapshot")).isEqualTo(Decimal128.parse("40.00"));
        assertThat(item.get("addedAt")).isInstanceOf(Date.class);
    }

    @Test
    void variantDeactivatedAfterAddIsUnavailableAndExcludedFromSubtotal() {
        String cartId = createCart();
        addItem(cartId, SHIRT_BLUE_M, 1);
        addItem(cartId, SHIRT_WHITE_L, 2);
        mongoTemplate.getCollection("product_variants").updateOne(new Document("_id", SHIRT_BLUE_M),
                new Document("$set", new Document("active", false)));

        CartApiTestResponse cart = send("GET", "/api/v1/cart", null, "X-Cart-Id", cartId);

        assertThat(cart.<List<String>>read("$.items[*].status")).containsExactly("UNAVAILABLE", "AVAILABLE");
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("80.00");
        assertThat(cart.<Integer>read("$.itemCount")).isEqualTo(3);
        assertProblem(send("PATCH", "/api/v1/cart/items/" + SHIRT_BLUE_M.toHexString(), "{\"quantity\":1}",
                "X-Cart-Id", cartId), 409, "VARIANT_UNAVAILABLE");
        assertThat(send("DELETE", "/api/v1/cart/items/" + SHIRT_BLUE_M.toHexString(), null, "X-Cart-Id", cartId)
                .status()).isEqualTo(200);
    }

    @Test
    void priceChangeAfterAddIsFlagged() {
        String cartId = createCart();
        addItem(cartId, SHIRT_BLUE_M, 1);
        mongoTemplate.getCollection("products").updateOne(new Document("_id", SHIRT),
                new Document("$set", new Document("salePrice", null)));

        CartApiTestResponse cart = send("GET", "/api/v1/cart", null, "X-Cart-Id", cartId);

        assertThat(cart.<Boolean>read("$.items[0].priceChanged")).isTrue();
        assertThat(cart.<String>read("$.items[0].unitPrice.amount")).isEqualTo("50.00");
        assertThat(cart.<Object>read("$.items[0].originalPrice")).isNull();
    }

    @Test
    void mergeAboveMaximumReturnsLimitWithMaxItemQuantity() {
        mongoTemplate.getCollection("product_variants").updateOne(new Document("_id", SHIRT_BLUE_M),
                new Document("$set", new Document("stockQuantity", 100)));
        String cartId = createCart();
        addItem(cartId, SHIRT_BLUE_M, 8);

        CartApiTestResponse response = addItem(cartId, SHIRT_BLUE_M, 3);

        assertProblem(response, 409, "CART_ITEM_LIMIT_EXCEEDED");
        assertThat(response.<Integer>read("$.maxItemQuantity")).isEqualTo(10);
    }

    @Test
    void unknownMalformedAndMissingCartIds() {
        assertProblem(send("GET", "/api/v1/cart", null, "X-Cart-Id", UUID.randomUUID().toString()), 404, "CART_NOT_FOUND");
        assertProblem(send("GET", "/api/v1/cart", null, "X-Cart-Id", "not-a-uuid"), 404, "CART_NOT_FOUND");
        CartApiTestResponse missing = send("GET", "/api/v1/cart", null);
        assertProblem(missing, 400, "VALIDATION_ERROR");
        assertThat(missing.<String>read("$.errors[0].field")).isEqualTo("X-Cart-Id");
    }

    @Test
    void nonIntegerOrNullQuantityIsRejectedOnQuantityFieldAndCartIsUnchanged() {
        String cartId = createCart();
        addItem(cartId, SHIRT_BLUE_M, 1);
        Document before = mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first();

        for (String quantity : List.of("1.5", "\"2\"", "true", "null")) {
            CartApiTestResponse added = cartApi().addItemWithBody(cartId,
                    "{\"variantId\":\"" + SHIRT_WHITE_L.toHexString() + "\",\"quantity\":" + quantity + "}");
            CartApiTestResponse patched = cartApi().updateQuantityWithBody(cartId, SHIRT_BLUE_M.toHexString(),
                    "{\"quantity\":" + quantity + "}");

            assertProblem(added, 400, "VALIDATION_ERROR");
            assertThat(added.<String>read("$.errors[0].field")).as(quantity).isEqualTo("quantity");
            assertProblem(patched, 400, "VALIDATION_ERROR");
            assertThat(patched.<String>read("$.errors[0].field")).as(quantity).isEqualTo("quantity");
        }
        assertThat(mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first()).isEqualTo(before);
    }

    @Test
    void quantityOutsideRangeReturnsFriendlyMessageWithConfiguredMaximum() {
        String cartId = createCart();
        addItem(cartId, SHIRT_BLUE_M, 1);

        for (int quantity : List.of(0, 11)) {
            CartApiTestResponse added = addItem(cartId, SHIRT_WHITE_L, quantity);
            CartApiTestResponse patched = cartApi().updateQuantity(cartId, SHIRT_BLUE_M.toHexString(), quantity);

            assertProblem(added, 400, "VALIDATION_ERROR");
            assertThat(added.<String>read("$.errors[0].message")).isEqualTo("Quantity must be between 1 and 10.");
            assertProblem(patched, 400, "VALIDATION_ERROR");
            assertThat(patched.<String>read("$.errors[0].field")).isEqualTo("quantity");
            assertThat(patched.<String>read("$.errors[0].message")).isEqualTo("Quantity must be between 1 and 10.");
        }
    }

    @Test
    void malformedVariantIdsNeverCauseServerErrors() {
        String cartId = createCart();

        assertProblem(send("POST", "/api/v1/cart/items", "{\"variantId\":\"not-an-id\",\"quantity\":1}",
                "X-Cart-Id", cartId), 409, "VARIANT_UNAVAILABLE");
        assertProblem(send("PATCH", "/api/v1/cart/items/not-an-id", "{\"quantity\":1}", "X-Cart-Id", cartId),
                404, "CART_ITEM_NOT_FOUND");
        assertProblem(send("DELETE", "/api/v1/cart/items/not-an-id", null, "X-Cart-Id", cartId),
                404, "CART_ITEM_NOT_FOUND");
    }

    @Test
    void staleCartVersionIsRejectedByOptimisticLocking() {
        String cartId = createCart();
        Cart stale = cartRepository.findById(cartId).orElseThrow();
        addItem(cartId, SHIRT_BLUE_M, 1);

        assertThatThrownBy(() -> cartRepository.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void inactiveCartExpiryTtlIndexExistsOnUpdatedAt() {
        List<Document> indexes = mongoTemplate.getCollection("carts").listIndexes().into(new ArrayList<>());

        assertThat(indexes).anySatisfy(index -> {
            assertThat(index.getString("name")).isEqualTo("ttl_carts_updated_at");
            assertThat(index.get("key", Document.class)).isEqualTo(new Document("updatedAt", 1));
            assertThat(((Number) index.get("expireAfterSeconds")).longValue()).isEqualTo(Duration.ofDays(30).toSeconds());
        });
    }

    @Test
    void preflightForCartWriteMethodsWithCartHeaderIsAllowed() {
        String itemPath = "/api/v1/cart/items/" + SHIRT_BLUE_M.toHexString();
        for (String[] pathAndMethod : List.of(new String[]{"/api/v1/cart", "POST"}, new String[]{"/api/v1/cart/items", "POST"},
                new String[]{itemPath, "PATCH"}, new String[]{itemPath, "DELETE"})) {
            CartApiTestResponse response = send("OPTIONS", pathAndMethod[0], null, "Origin", ALLOWED_ORIGIN,
                    "Access-Control-Request-Method", pathAndMethod[1],
                    "Access-Control-Request-Headers", "content-type,x-cart-id");

            assertThat(response.status()).as(pathAndMethod[1]).isEqualTo(200);
            assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            assertThat(response.header("Access-Control-Allow-Methods")).contains(pathAndMethod[1]);
            assertThat(response.header("Access-Control-Allow-Headers")).containsIgnoringCase("x-cart-id");
            assertThat(response.header("Access-Control-Allow-Credentials")).isNull();
        }
    }

    @Test
    void catalogPreflightAllowsCartHeaderButStaysReadOnly() {
        CartApiTestResponse read = send("OPTIONS", "/api/v1/products", null, "Origin", ALLOWED_ORIGIN,
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "x-cart-id");
        CartApiTestResponse write = send("OPTIONS", "/api/v1/products", null, "Origin", ALLOWED_ORIGIN,
                "Access-Control-Request-Method", "PATCH");

        assertThat(read.status()).isEqualTo(200);
        assertThat(read.header("Access-Control-Allow-Methods")).doesNotContain("POST");
        assertThat(write.status()).isEqualTo(403);
    }

    @Test
    void preflightWithUnlistedHeaderIsRejected() {
        CartApiTestResponse response = send("OPTIONS", "/api/v1/cart", null, "Origin", ALLOWED_ORIGIN,
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");

        assertThat(response.status()).isEqualTo(403);
    }

    @Test
    void responsesNeverExposeStockOrInternalFields() {
        String cartId = createCart();
        CartApiTestResponse response = addItem(cartId, SHIRT_BLUE_M, 1);

        assertThat(response.body()).doesNotContain("stockQuantity").doesNotContain("sku")
                .doesNotContain("version").doesNotContain("unitPriceSnapshot");
        assertThat(JsonPath.<Map<String, Object>>read(response.body(), "$.items[0]")).doesNotContainKey("addedAt");
    }
}
