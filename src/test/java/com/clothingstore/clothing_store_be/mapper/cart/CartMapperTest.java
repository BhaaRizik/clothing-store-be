package com.clothingstore.clothing_store_be.mapper.cart;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.cart.CartItemResponse;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.dto.product.ProductColorResponse;
import com.clothingstore.clothing_store_be.mapper.common.MoneyMapperImpl;
import com.clothingstore.clothing_store_be.mapper.product.ProductVariantMapperImpl;
import com.clothingstore.clothing_store_be.model.cart.CartItem;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import com.clothingstore.clothing_store_be.model.cart.PricedCart;
import com.clothingstore.clothing_store_be.model.cart.PricedCartItem;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartMapperTest {

    private static final StoreProperties STORE = new StoreProperties("ILS");
    private static final CartItem ITEM = new CartItem("p1", "v1", 2, new BigDecimal("40.00"), Instant.EPOCH);

    private final CartMapper cartMapper = new CartMapperImpl(new MoneyMapperImpl(), new ProductVariantMapperImpl());

    private static Product product(List<ProductImage> images) {
        return new Product("p1", "oxford-shirt", "Oxford Shirt", "Desc", "c1", null, images,
                new BigDecimal("50"), new BigDecimal("40"), true, false, Instant.EPOCH, Instant.EPOCH);
    }

    private static ProductVariant variant(ProductColor color) {
        return new ProductVariant("v1", "p1", "SKU", "M", color, 5, true, 1, Instant.EPOCH, Instant.EPOCH);
    }

    private static PricedCartItem pricedItem(Product product, ProductVariant variant) {
        return new PricedCartItem(ITEM, product, variant, new BigDecimal("40"), new BigDecimal("50"),
                new BigDecimal("80"), CartItemStatus.AVAILABLE, true);
    }

    @Test
    void mapsEveryCartItemField() {
        Product product = product(List.of(new ProductImage("/img/blue.svg", "Blue shirt", "Blue")));
        CartItemResponse response = cartMapper.toCartItemResponse(
                pricedItem(product, variant(new ProductColor("Blue", "#0000FF"))), STORE);

        assertThat(response).isEqualTo(new CartItemResponse("v1", "p1", "oxford-shirt", "Oxford Shirt",
                new ImageResponse("/img/blue.svg", "Blue shirt"), "M", new ProductColorResponse("Blue", "#0000FF"), 2,
                new Money("40.00", "ILS"), new Money("50.00", "ILS"), new Money("80.00", "ILS"),
                CartItemStatus.AVAILABLE, true));
    }

    @Test
    void imagePrefersVariantColorThenUntaggedThenFirst() {
        ProductImage red = new ProductImage("/img/red.svg", "Red", "Red");
        ProductImage untagged = new ProductImage("/img/plain.svg", "Plain", null);
        ProductImage blue = new ProductImage("/img/blue.svg", "Blue", "Blue");
        ProductVariant blueVariant = variant(new ProductColor("Blue", null));

        assertThat(cartMapper.toCartItemResponse(pricedItem(product(List.of(red, untagged, blue)), blueVariant), STORE)
                .image().url()).isEqualTo("/img/blue.svg");
        assertThat(cartMapper.toCartItemResponse(pricedItem(product(List.of(red, untagged)), blueVariant), STORE)
                .image().url()).isEqualTo("/img/plain.svg");
        assertThat(cartMapper.toCartItemResponse(pricedItem(product(List.of(red)), blueVariant), STORE)
                .image().url()).isEqualTo("/img/red.svg");
    }

    @Test
    void variantWithoutColorUsesUntaggedImage() {
        ProductImage red = new ProductImage("/img/red.svg", "Red", "Red");
        ProductImage untagged = new ProductImage("/img/plain.svg", "Plain", null);

        assertThat(cartMapper.toCartItemResponse(pricedItem(product(List.of(red, untagged)), variant(null)), STORE)
                .image().url()).isEqualTo("/img/plain.svg");
    }

    @Test
    void imageIsNullWhenProductHasNoImages() {
        assertThat(cartMapper.toCartItemResponse(pricedItem(product(null), variant(null)), STORE).image()).isNull();
        assertThat(cartMapper.toCartItemResponse(pricedItem(product(List.of()), variant(null)), STORE).image()).isNull();
    }

    @Test
    void missingProductAndVariantMapToNullDetails() {
        PricedCartItem missing = new PricedCartItem(ITEM, null, null, null, null, null, CartItemStatus.UNAVAILABLE, false);

        assertThat(cartMapper.toCartItemResponse(missing, STORE)).isEqualTo(new CartItemResponse("v1", "p1", null, null,
                null, null, null, 2, null, null, null, CartItemStatus.UNAVAILABLE, false));
    }

    @Test
    void mapsCartEnvelopeWithFormattedSubtotal() {
        CartResponse response = cartMapper.toCartResponse(
                new PricedCart("cart-1", List.of(), 0, BigDecimal.ZERO, 10), STORE);

        assertThat(response).isEqualTo(new CartResponse("cart-1", List.of(), 0, new Money("0.00", "ILS"), 10));
    }

    @Test
    void nullItemListMapsToEmptyList() {
        assertThat(cartMapper.toCartResponse(new PricedCart("cart-1", null, 0, BigDecimal.ONE, 10), STORE).items())
                .isNotNull().isEmpty();
    }
}
