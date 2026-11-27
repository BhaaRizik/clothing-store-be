package com.clothingstore.clothing_store_be.service.cart;

import com.clothingstore.clothing_store_be.exception.cart.InsufficientStockException;
import com.clothingstore.clothing_store_be.model.cart.Cart;
import com.clothingstore.clothing_store_be.model.cart.CartItem;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import com.clothingstore.clothing_store_be.model.cart.PricedCartItem;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartServiceUtilsTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final String PRODUCT_ID = "650000000000000000000b01";
    private static final String VARIANT_ID = "650000000000000000000c01";
    private static final String OTHER_VARIANT_ID = "650000000000000000000c02";

    @Test
    void requireStockForAllowsQuantityUpToStockAndRejectsMore() {
        ProductVariant variant = variant(VARIANT_ID, 3, true);

        assertThatNoException().isThrownBy(() -> CartServiceUtils.requireStockFor(variant, 3));
        assertThatThrownBy(() -> CartServiceUtils.requireStockFor(variant, 4))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void cartItemsOfReturnsEmptyListWhenItemsAreMissing() {
        Cart cart = new Cart("cart", null, NOW, NOW, 1L);

        assertThat(CartServiceUtils.cartItemsOf(cart)).isEmpty();
    }

    @Test
    void withItemsKeepsIdentityCreationTimeAndVersion() {
        Cart cart = new Cart("cart", List.of(), NOW.minusSeconds(60), NOW.minusSeconds(60), 4L);
        List<CartItem> items = List.of(item(VARIANT_ID, 1, null));

        Cart updated = CartServiceUtils.withItems(cart, items, NOW);

        assertThat(updated).isEqualTo(new Cart("cart", items, NOW.minusSeconds(60), NOW, 4L));
    }

    @Test
    void findReplaceAndAppendWorkByVariantId() {
        CartItem first = item(VARIANT_ID, 1, null);
        CartItem second = item(OTHER_VARIANT_ID, 2, null);
        CartItem replacement = item(VARIANT_ID, 5, null);

        assertThat(CartServiceUtils.findCartItem(List.of(first, second), OTHER_VARIANT_ID)).contains(second);
        assertThat(CartServiceUtils.findCartItem(List.of(first), OTHER_VARIANT_ID)).isEmpty();
        assertThat(CartServiceUtils.replaceCartItem(List.of(first, second), replacement))
                .containsExactly(replacement, second);
        assertThat(CartServiceUtils.appendCartItem(List.of(first), second)).containsExactly(first, second);
    }

    @Test
    void currentUnitPricePrefersSalePriceOverBasePrice() {
        assertThat(CartServiceUtils.currentUnitPrice(product(new BigDecimal("100"), new BigDecimal("80"), true)))
                .contains(new BigDecimal("80"));
        assertThat(CartServiceUtils.currentUnitPrice(product(new BigDecimal("100"), null, true)))
                .contains(new BigDecimal("100"));
    }

    @Test
    void toMoneyScaleRoundsHalfUpToTwoDecimals() {
        assertThat(CartServiceUtils.toMoneyScale(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(CartServiceUtils.toMoneyScale(new BigDecimal("10")).scale()).isEqualTo(2);
    }

    @Test
    void stockOfAndIsActiveVariantTreatMissingValuesAsUnavailable() {
        ProductVariant missingValues = variant(VARIANT_ID, null, null);

        assertThat(CartServiceUtils.stockOf(missingValues)).isZero();
        assertThat(CartServiceUtils.isActiveVariant(missingValues)).isFalse();
    }

    @Test
    void statusOfReflectsProductVariantAndStock() {
        Product active = product(new BigDecimal("50"), null, true);
        Product inactive = product(new BigDecimal("50"), null, false);
        CartItem twoUnits = item(VARIANT_ID, 2, null);

        assertThat(CartServiceUtils.statusOf(twoUnits, active, variant(VARIANT_ID, 5, true)))
                .isEqualTo(CartItemStatus.AVAILABLE);
        assertThat(CartServiceUtils.statusOf(twoUnits, active, variant(VARIANT_ID, 1, true)))
                .isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);
        assertThat(CartServiceUtils.statusOf(twoUnits, active, variant(VARIANT_ID, 0, true)))
                .isEqualTo(CartItemStatus.UNAVAILABLE);
        assertThat(CartServiceUtils.statusOf(twoUnits, active, variant(VARIANT_ID, 5, false)))
                .isEqualTo(CartItemStatus.UNAVAILABLE);
        assertThat(CartServiceUtils.statusOf(twoUnits, inactive, variant(VARIANT_ID, 5, true)))
                .isEqualTo(CartItemStatus.UNAVAILABLE);
        assertThat(CartServiceUtils.statusOf(twoUnits, null, null))
                .isEqualTo(CartItemStatus.UNAVAILABLE);
    }

    @Test
    void priceCartItemComputesLivePricesAndPriceChangedFlag() {
        Product onSale = product(new BigDecimal("100"), new BigDecimal("79.9"), true);
        CartItem item = item(VARIANT_ID, 3, new BigDecimal("100.00"));

        PricedCartItem priced = CartServiceUtils.priceCartItem(item, onSale, variant(VARIANT_ID, 10, true));

        assertThat(priced.unitPrice()).isEqualByComparingTo("79.90");
        assertThat(priced.originalPrice()).isEqualByComparingTo("100");
        assertThat(priced.lineTotal()).isEqualByComparingTo("239.70");
        assertThat(priced.priceChanged()).isTrue();
        assertThat(priced.status()).isEqualTo(CartItemStatus.AVAILABLE);
    }

    @Test
    void priceCartItemWithoutProductHasNoPricesAndIsUnavailable() {
        PricedCartItem priced = CartServiceUtils.priceCartItem(item(VARIANT_ID, 1, new BigDecimal("10")), null, null);

        assertThat(priced.unitPrice()).isNull();
        assertThat(priced.originalPrice()).isNull();
        assertThat(priced.lineTotal()).isNull();
        assertThat(priced.priceChanged()).isFalse();
        assertThat(priced.status()).isEqualTo(CartItemStatus.UNAVAILABLE);
    }

    private static CartItem item(String variantId, int quantity, BigDecimal snapshot) {
        return new CartItem(PRODUCT_ID, variantId, quantity, snapshot, NOW);
    }

    private static Product product(BigDecimal basePrice, BigDecimal salePrice, boolean active) {
        return new Product(PRODUCT_ID, "shirt", "Shirt", null, null, null, List.of(), basePrice, salePrice,
                active, false, NOW, NOW);
    }

    private static ProductVariant variant(String id, Integer stock, Boolean active) {
        return new ProductVariant(id, PRODUCT_ID, "SKU-" + id, "M", null, stock, active, 1, NOW, NOW);
    }
}
