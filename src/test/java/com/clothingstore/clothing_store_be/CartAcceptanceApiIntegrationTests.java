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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.cors.allowed-origins=https://shop.example.test",
        "app.cart.max-item-quantity=10",
        "app.cart.inactive-cart-expiry=30d"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CartAcceptanceApiIntegrationTests {

    private static final String STORE_CURRENCY = "ILS";
    private static final Date CREATED = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
    private static final ObjectId CATEGORY = new ObjectId("680000000000000000000a01");
    private static final ObjectId TEE = new ObjectId("680000000000000000000b01");
    private static final ObjectId JACKET = new ObjectId("680000000000000000000b02");
    private static final ObjectId SOCKS = new ObjectId("680000000000000000000b03");
    private static final String TEE_ONE_SIZE = "680000000000000000000c01";
    private static final String TEE_RED_S = "680000000000000000000c02";
    private static final String JACKET_BLACK_L = "680000000000000000000c03";
    private static final String SOCKS_PACK = "680000000000000000000c04";

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    private CartApiTestClient cartApi;

    @BeforeEach
    void seedCatalog() {
        cartApi = new CartApiTestClient(port);
        List.of("carts", "categories", "products", "product_variants")
                .forEach(collection -> mongoTemplate.getCollection(collection).deleteMany(new Document()));
        mongoTemplate.getCollection("categories").insertOne(new Document("_id", CATEGORY).append("slug", "unisex")
                .append("name", "Unisex").append("displayOrder", 1).append("active", true)
                .append("createdAt", CREATED).append("updatedAt", CREATED));
        mongoTemplate.getCollection("products").insertMany(List.of(
                product(TEE, "basic-tee", "Basic Tee", "49.90", null,
                        List.of(new Document("url", "/img/tee.svg").append("alt", "Tee"),
                                new Document("url", "/img/tee-red.svg").append("alt", "Red tee").append("color", "Red"))),
                product(JACKET, "rain-jacket", "Rain Jacket", "99.90", "79.90",
                        List.of(new Document("url", "/img/jacket.svg").append("alt", "Jacket").append("color", "Black"))),
                product(SOCKS, "wool-socks", "Wool Socks", "10.005", null, List.of())));
        mongoTemplate.getCollection("product_variants").insertMany(List.of(
                variant(TEE_ONE_SIZE, TEE, "TEE-OS", null, null, 5),
                variant(TEE_RED_S, TEE, "TEE-RED-S", "S", new Document("name", "Red").append("hexCode", "#FF0000"), 2),
                variant(JACKET_BLACK_L, JACKET, "JKT-BLK-L", "L",
                        new Document("name", "Black").append("hexCode", "#000000"), 10),
                variant(SOCKS_PACK, SOCKS, "SOCKS-PACK", "M", null, 20)));
    }

    private static Document product(ObjectId id, String slug, String name, String basePrice, String salePrice,
            List<Document> images) {
        return new Document("_id", id).append("slug", slug).append("name", name).append("description", name)
                .append("categoryId", CATEGORY).append("basePrice", Decimal128.parse(basePrice))
                .append("salePrice", salePrice == null ? null : Decimal128.parse(salePrice))
                .append("images", images).append("active", true).append("featured", false)
                .append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private static Document variant(String id, ObjectId productId, String sku, String size, Document color, int stock) {
        return new Document("_id", new ObjectId(id)).append("productId", productId).append("sku", sku)
                .append("size", size).append("color", color).append("stockQuantity", stock).append("active", true)
                .append("displayOrder", 1).append("createdAt", CREATED).append("updatedAt", CREATED);
    }

    private void updateVariant(String variantId, Document changes) {
        mongoTemplate.getCollection("product_variants")
                .updateOne(new Document("_id", new ObjectId(variantId)), new Document("$set", changes));
    }

    private void updateProduct(ObjectId productId, Document changes) {
        mongoTemplate.getCollection("products").updateOne(new Document("_id", productId), new Document("$set", changes));
    }

    private Document storedCart(String cartId) {
        return mongoTemplate.getCollection("carts").find(new Document("_id", cartId)).first();
    }

    private static void assertProblem(CartApiTestResponse response, int status, String code) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.header("Content-Type")).startsWith("application/problem+json");
        assertThat(response.<String>read("$.code")).isEqualTo(code);
    }

    @Test
    void subtotalSumsCatalogPricesOfRegularAndSaleLinesAndIgnoresClientPrices() {
        String cartId = cartApi.createCart();
        cartApi.addItemWithBody(cartId, "{\"variantId\":\"" + TEE_ONE_SIZE
                + "\",\"quantity\":2,\"price\":\"0.01\",\"unitPrice\":{\"amount\":\"0.01\"},\"subtotal\":\"0.00\"}");

        CartApiTestResponse cart = cartApi.addItemWithBody(cartId, "{\"variantId\":\"" + JACKET_BLACK_L
                + "\",\"quantity\":1,\"price\":\"1.00\",\"lineTotal\":\"1.00\"}");

        assertThat(cart.status()).isEqualTo(200);
        assertThat(cart.<List<String>>read("$.items[*].unitPrice.amount")).containsExactly("49.90", "79.90");
        assertThat(cart.<List<String>>read("$.items[*].lineTotal.amount")).containsExactly("99.80", "79.90");
        assertThat(cart.<Object>read("$.items[0].originalPrice")).isNull();
        assertThat(cart.<String>read("$.items[1].originalPrice.amount")).isEqualTo("99.90");
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("179.70");
        assertThat(cart.<Integer>read("$.itemCount")).isEqualTo(3);
        assertThat(cart.<List<String>>read("$..currency")).hasSize(6).containsOnly(STORE_CURRENCY);
        assertThat(storedCart(cartId).getList("items", Document.class))
                .extracting(item -> item.get("unitPriceSnapshot"))
                .containsExactly(Decimal128.parse("49.90"), Decimal128.parse("79.90"));
    }

    @Test
    void variantWithoutSizeOrColorReturnsNullSizeAndColorWithUntaggedImage() {
        String cartId = cartApi.createCart();

        CartApiTestResponse cart = cartApi.addItem(cartId, TEE_ONE_SIZE, 1);

        assertThat(cart.<Object>read("$.items[0].size")).isNull();
        assertThat(cart.<Object>read("$.items[0].color")).isNull();
        assertThat(cart.<String>read("$.items[0].image.url")).isEqualTo("/img/tee.svg");
        assertThat(cart.<String>read("$.items[0].productId")).isEqualTo(TEE.toHexString());
        assertThat(cart.<String>read("$.items[0].productSlug")).isEqualTo("basic-tee");
        assertThat(cart.<String>read("$.items[0].productName")).isEqualTo("Basic Tee");
        assertThat(cart.<String>read("$.items[0].status")).isEqualTo("AVAILABLE");
        assertThat(cart.<Boolean>read("$.items[0].priceChanged")).isFalse();
    }

    @Test
    void variantWithSizeAndColorReturnsThemWithColorTaggedImage() {
        String cartId = cartApi.createCart();

        CartApiTestResponse cart = cartApi.addItem(cartId, TEE_RED_S, 1);

        assertThat(cart.<String>read("$.items[0].size")).isEqualTo("S");
        assertThat(cart.<String>read("$.items[0].color.name")).isEqualTo("Red");
        assertThat(cart.<String>read("$.items[0].color.hexCode")).isEqualTo("#FF0000");
        assertThat(cart.<String>read("$.items[0].image.url")).isEqualTo("/img/tee-red.svg");
    }

    @Test
    void quantityExactlyEqualToStockIsAcceptedAndOneMoreIsRejectedWithCartUnchanged() {
        String cartId = cartApi.createCart();
        CartApiTestResponse accepted = cartApi.addItem(cartId, TEE_RED_S, 2);
        Document before = storedCart(cartId);

        CartApiTestResponse rejected = cartApi.addItem(cartId, TEE_RED_S, 1);

        assertThat(accepted.status()).isEqualTo(200);
        assertThat(accepted.<Integer>read("$.items[0].quantity")).isEqualTo(2);
        assertProblem(rejected, 409, "INSUFFICIENT_STOCK");
        assertThat(rejected.<String>read("$.detail")).doesNotContain("2").doesNotContain("3");
        assertThat(storedCart(cartId)).isEqualTo(before);
        assertThat(cartApi.getCart(cartId).<Integer>read("$.items[0].quantity")).isEqualTo(2);
    }

    @Test
    void unavailableVariantsAreRejectedWithCartUnchanged() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 1);
        updateVariant(TEE_RED_S, new Document("active", false));
        updateVariant(SOCKS_PACK, new Document("stockQuantity", 0));
        updateProduct(JACKET, new Document("active", false));
        Document before = storedCart(cartId);

        assertProblem(cartApi.addItem(cartId, TEE_RED_S, 1), 409, "VARIANT_UNAVAILABLE");
        assertProblem(cartApi.addItem(cartId, SOCKS_PACK, 1), 409, "VARIANT_UNAVAILABLE");
        assertProblem(cartApi.addItem(cartId, JACKET_BLACK_L, 1), 409, "VARIANT_UNAVAILABLE");
        assertProblem(cartApi.addItem(cartId, new ObjectId().toHexString(), 1), 409, "VARIANT_UNAVAILABLE");
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @Test
    void variantWhoseProductWasDeletedIsRejected() {
        String cartId = cartApi.createCart();
        mongoTemplate.getCollection("products").deleteOne(new Document("_id", JACKET));

        assertProblem(cartApi.addItem(cartId, JACKET_BLACK_L, 1), 409, "VARIANT_UNAVAILABLE");
        assertThat(storedCart(cartId).getList("items", Document.class)).isEmpty();
    }

    @Test
    void quantityAtConfiguredMaximumIsAcceptedAndOneAboveIsRejectedForAddAndUpdate() {
        String cartId = cartApi.createCart();

        CartApiTestResponse atMaximum = cartApi.addItem(cartId, JACKET_BLACK_L, 10);
        Document before = storedCart(cartId);
        CartApiTestResponse addAboveMaximum = cartApi.addItem(cartId, SOCKS_PACK, 11);
        CartApiTestResponse updateAboveMaximum = cartApi.updateQuantity(cartId, JACKET_BLACK_L, 11);

        assertThat(atMaximum.status()).isEqualTo(200);
        assertThat(atMaximum.<Integer>read("$.items[0].quantity")).isEqualTo(10);
        assertProblem(addAboveMaximum, 400, "VALIDATION_ERROR");
        assertProblem(updateAboveMaximum, 400, "VALIDATION_ERROR");
        assertThat(updateAboveMaximum.<String>read("$.errors[0].field")).isEqualTo("quantity");
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @Test
    void updateIncreasesAndDecreasesQuantityKeepingLineOrderAndRecalculatingSubtotal() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 1);
        cartApi.addItem(cartId, JACKET_BLACK_L, 1);

        CartApiTestResponse increased = cartApi.updateQuantity(cartId, TEE_ONE_SIZE, 5);
        CartApiTestResponse decreased = cartApi.updateQuantity(cartId, TEE_ONE_SIZE, 1);

        assertThat(increased.status()).isEqualTo(200);
        assertThat(increased.<List<String>>read("$.items[*].variantId")).containsExactly(TEE_ONE_SIZE, JACKET_BLACK_L);
        assertThat(increased.<Integer>read("$.items[0].quantity")).isEqualTo(5);
        assertThat(increased.<String>read("$.items[0].lineTotal.amount")).isEqualTo("249.50");
        assertThat(increased.<String>read("$.subtotal.amount")).isEqualTo("329.40");
        assertThat(increased.<Integer>read("$.itemCount")).isEqualTo(6);
        assertThat(decreased.<List<String>>read("$.items[*].variantId")).containsExactly(TEE_ONE_SIZE, JACKET_BLACK_L);
        assertThat(decreased.<String>read("$.subtotal.amount")).isEqualTo("129.80");
        assertThat(decreased.<Integer>read("$.itemCount")).isEqualTo(2);
    }

    @Test
    void rejectedUpdatesLeaveCartUnchanged() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_RED_S, 1);
        Document before = storedCart(cartId);

        assertProblem(cartApi.updateQuantity(cartId, TEE_RED_S, 3), 409, "INSUFFICIENT_STOCK");
        assertProblem(cartApi.updateQuantity(cartId, TEE_RED_S, 0), 400, "VALIDATION_ERROR");
        assertProblem(cartApi.updateQuantity(cartId, TEE_RED_S, -2), 400, "VALIDATION_ERROR");
        assertProblem(cartApi.updateQuantityWithBody(cartId, TEE_RED_S, "{\"quantity\":\"two\"}"), 400,
                "VALIDATION_ERROR");
        assertProblem(cartApi.updateQuantityWithBody(cartId, TEE_RED_S, "{"), 400, "VALIDATION_ERROR");
        assertThat(storedCart(cartId)).isEqualTo(before);
    }

    @Test
    void updateToExactlyTheStockIsAccepted() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 1);

        CartApiTestResponse cart = cartApi.updateQuantity(cartId, TEE_ONE_SIZE, 5);

        assertThat(cart.status()).isEqualTo(200);
        assertThat(cart.<Integer>read("$.items[0].quantity")).isEqualTo(5);
        assertThat(cart.<String>read("$.items[0].status")).isEqualTo("AVAILABLE");
    }

    @Test
    void removingTheLastLineReturnsEmptyCartWithZeroSubtotal() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, JACKET_BLACK_L, 2);

        CartApiTestResponse cart = cartApi.removeItem(cartId, JACKET_BLACK_L);

        assertThat(cart.status()).isEqualTo(200);
        assertThat(cart.<List<Object>>read("$.items")).isEmpty();
        assertThat(cart.<Integer>read("$.itemCount")).isZero();
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("0.00");
        assertThat(cart.<String>read("$.subtotal.currency")).isEqualTo(STORE_CURRENCY);
        assertThat(cartApi.getCart(cartId).<List<Object>>read("$.items")).isEmpty();
    }

    @Test
    void removingLineFromOneCartDoesNotChangeAnotherCartWithTheSameVariant() {
        String cartA = cartApi.createCart();
        String cartB = cartApi.createCart();
        cartApi.addItem(cartA, JACKET_BLACK_L, 1);
        cartApi.addItem(cartB, JACKET_BLACK_L, 2);
        Document cartBBefore = storedCart(cartB);

        CartApiTestResponse removed = cartApi.removeItem(cartA, JACKET_BLACK_L);

        assertThat(removed.status()).isEqualTo(200);
        assertThat(removed.<String>read("$.id")).isEqualTo(cartA);
        assertThat(removed.<List<Object>>read("$.items")).isEmpty();
        assertThat(storedCart(cartB)).isEqualTo(cartBBefore);
        assertThat(cartApi.getCart(cartB).<Integer>read("$.items[0].quantity")).isEqualTo(2);
    }

    @Test
    void lineThatExistsOnlyInAnotherCartIsItemNotFoundAndNoCartChanges() {
        String cartA = cartApi.createCart();
        String cartB = cartApi.createCart();
        cartApi.addItem(cartA, TEE_ONE_SIZE, 1);
        cartApi.addItem(cartB, JACKET_BLACK_L, 1);
        Document cartABefore = storedCart(cartA);
        Document cartBBefore = storedCart(cartB);

        assertProblem(cartApi.updateQuantity(cartA, JACKET_BLACK_L, 2), 404, "CART_ITEM_NOT_FOUND");
        assertProblem(cartApi.removeItem(cartA, JACKET_BLACK_L), 404, "CART_ITEM_NOT_FOUND");
        assertThat(storedCart(cartA)).isEqualTo(cartABefore);
        assertThat(storedCart(cartB)).isEqualTo(cartBBefore);
    }

    @Test
    void cartIdReturnsOnlyThatCartsLines() {
        String cartA = cartApi.createCart();
        String cartB = cartApi.createCart();
        cartApi.addItem(cartA, TEE_ONE_SIZE, 1);
        cartApi.addItem(cartB, JACKET_BLACK_L, 3);

        CartApiTestResponse cart = cartApi.getCart(cartA);

        assertThat(cart.<String>read("$.id")).isEqualTo(cartA);
        assertThat(cart.<List<String>>read("$.items[*].variantId")).containsExactly(TEE_ONE_SIZE);
        assertThat(cart.<Integer>read("$.itemCount")).isEqualTo(1);
        assertThat(cart.body()).doesNotContain(cartB).doesNotContain(JACKET_BLACK_L);
    }

    @Test
    void expiredCartIsNotFoundExactlyLikeAnUnknownId() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 1);
        mongoTemplate.getCollection("carts").deleteOne(new Document("_id", cartId));

        CartApiTestResponse expired = cartApi.getCart(cartId);
        CartApiTestResponse unknown = cartApi.getCart(UUID.randomUUID().toString());

        assertProblem(expired, 404, "CART_NOT_FOUND");
        assertThat(expired.body()).isEqualTo(unknown.body());
        assertProblem(cartApi.addItem(cartId, TEE_ONE_SIZE, 1), 404, "CART_NOT_FOUND");
        assertProblem(cartApi.updateQuantity(cartId, TEE_ONE_SIZE, 1), 404, "CART_NOT_FOUND");
        assertProblem(cartApi.removeItem(cartId, TEE_ONE_SIZE), 404, "CART_NOT_FOUND");
        assertThat(storedCart(cartId)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "00000000-0000-0000-0000-00000000000", "../carts", "{\"$ne\":null}"})
    void malformedCartIdsAreNotFound(String cartId) {
        cartApi.createCart();

        assertProblem(cartApi.getCart(cartId), 404, "CART_NOT_FOUND");
    }

    @Test
    void productDeactivatedAfterAddIsUnavailableAndExcludedFromSubtotal() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 2);
        cartApi.addItem(cartId, JACKET_BLACK_L, 1);
        updateProduct(TEE, new Document("active", false));

        CartApiTestResponse cart = cartApi.getCart(cartId);

        assertThat(cart.<List<String>>read("$.items[*].status")).containsExactly("UNAVAILABLE", "AVAILABLE");
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("79.90");
        assertThat(cart.<Integer>read("$.itemCount")).isEqualTo(3);
        assertProblem(cartApi.updateQuantity(cartId, TEE_ONE_SIZE, 1), 409, "VARIANT_UNAVAILABLE");
    }

    @Test
    void productDeletedAfterAddIsUnavailableWithNullCatalogDetails() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 1);
        cartApi.addItem(cartId, JACKET_BLACK_L, 1);
        mongoTemplate.getCollection("products").deleteOne(new Document("_id", JACKET));

        CartApiTestResponse cart = cartApi.getCart(cartId);

        assertThat(cart.status()).isEqualTo(200);
        assertThat(cart.<String>read("$.items[1].status")).isEqualTo("UNAVAILABLE");
        assertThat(cart.<Object>read("$.items[1].productName")).isNull();
        assertThat(cart.<Object>read("$.items[1].unitPrice")).isNull();
        assertThat(cart.<Object>read("$.items[1].lineTotal")).isNull();
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("49.90");
        assertThat(cartApi.removeItem(cartId, JACKET_BLACK_L).status()).isEqualTo(200);
    }

    @Test
    void stockDroppedBelowQuantityIsInsufficientStockAndStaysInSubtotal() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 4);
        updateVariant(TEE_ONE_SIZE, new Document("stockQuantity", 3));

        CartApiTestResponse cart = cartApi.getCart(cartId);
        CartApiTestResponse reduced = cartApi.updateQuantity(cartId, TEE_ONE_SIZE, 3);

        assertThat(cart.<String>read("$.items[0].status")).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("199.60");
        assertThat(cart.body()).doesNotContain("stockQuantity");
        assertThat(reduced.status()).isEqualTo(200);
        assertThat(reduced.<String>read("$.items[0].status")).isEqualTo("AVAILABLE");
    }

    @Test
    void stockDroppedToZeroAfterAddIsUnavailable() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 1);
        updateVariant(TEE_ONE_SIZE, new Document("stockQuantity", 0));

        CartApiTestResponse cart = cartApi.getCart(cartId);

        assertThat(cart.<String>read("$.items[0].status")).isEqualTo("UNAVAILABLE");
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("0.00");
    }

    @Test
    void saleStartedAfterAddShowsSalePriceWithOriginalPriceAndFlagsPriceChange() {
        String cartId = cartApi.createCart();
        cartApi.addItem(cartId, TEE_ONE_SIZE, 2);
        updateProduct(TEE, new Document("salePrice", Decimal128.parse("39.90")));

        CartApiTestResponse cart = cartApi.getCart(cartId);

        assertThat(cart.<String>read("$.items[0].unitPrice.amount")).isEqualTo("39.90");
        assertThat(cart.<String>read("$.items[0].originalPrice.amount")).isEqualTo("49.90");
        assertThat(cart.<String>read("$.items[0].lineTotal.amount")).isEqualTo("79.80");
        assertThat(cart.<Boolean>read("$.items[0].priceChanged")).isTrue();
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("79.80");
    }

    @Test
    void unitPriceIsRoundedHalfUpToTwoDecimalsBeforeMultiplying() {
        String cartId = cartApi.createCart();

        CartApiTestResponse cart = cartApi.addItem(cartId, SOCKS_PACK, 3);

        assertThat(cart.<String>read("$.items[0].unitPrice.amount")).isEqualTo("10.01");
        assertThat(cart.<String>read("$.items[0].lineTotal.amount")).isEqualTo("30.03");
        assertThat(cart.<String>read("$.subtotal.amount")).isEqualTo("30.03");
        assertThat(cart.<Boolean>read("$.items[0].priceChanged")).isFalse();
    }

    @Test
    void concurrentAddsToTheSameCartNeverLoseUpdatesOrFailWithServerErrors() throws Exception {
        String cartId = cartApi.createCart();
        int requests = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<CartApiTestResponse> responses;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<CartApiTestResponse>> futures = IntStream.range(0, requests)
                    .mapToObj(index -> executor.submit(() -> {
                        start.await();
                        return cartApi.addItem(cartId, SOCKS_PACK, 1);
                    }))
                    .toList();
            start.countDown();
            List<CartApiTestResponse> collected = new ArrayList<>();
            for (Future<CartApiTestResponse> future : futures) {
                collected.add(future.get());
            }
            responses = collected;
        }

        long succeeded = responses.stream().filter(response -> response.status() == 200).count();
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isIn(200, 409);
            if (response.status() == 409) {
                assertThat(response.<String>read("$.code")).isEqualTo("CART_CONFLICT");
            }
        });
        assertThat(succeeded).isPositive();
        int storedQuantity = cartApi.getCart(cartId).<Integer>read("$.items[0].quantity");
        assertThat(storedQuantity).isEqualTo((int) succeeded);
    }
}
