package com.clothingstore.clothing_store_be.util;

import com.clothingstore.clothing_store_be.constants.MoneyConstants;
import com.clothingstore.clothing_store_be.exception.cart.InsufficientStockException;
import com.clothingstore.clothing_store_be.model.cart.Cart;
import com.clothingstore.clothing_store_be.model.cart.CartItem;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import com.clothingstore.clothing_store_be.model.cart.PricedCartItem;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CartServiceUtils {

    public static void requireStockFor(ProductVariant variant, int quantity) {
        if (quantity > stockOf(variant)) {
            throw new InsufficientStockException();
        }
    }

    public static Cart withItems(Cart cart, List<CartItem> items, Instant updatedAt) {
        return new Cart(cart.id(), items, cart.createdAt(), updatedAt, cart.version());
    }

    public static List<CartItem> cartItemsOf(Cart cart) {
        return Optional.ofNullable(cart.items()).orElseGet(List::of);
    }

    public static Optional<CartItem> findCartItem(List<CartItem> items, String variantId) {
        return items.stream().filter(item -> Objects.equals(item.variantId(), variantId)).findFirst();
    }

    public static List<CartItem> replaceCartItem(List<CartItem> items, CartItem updatedItem) {
        return items.stream()
                .map(item -> Objects.equals(item.variantId(), updatedItem.variantId()) ? updatedItem : item)
                .toList();
    }

    public static List<CartItem> appendCartItem(List<CartItem> items, CartItem newItem) {
        return Stream.concat(items.stream(), Stream.of(newItem)).toList();
    }

    public static PricedCartItem priceCartItem(CartItem item, Product product, ProductVariant variant) {
        Optional<Product> currentProduct = Optional.ofNullable(product);
        Optional<BigDecimal> unitPrice = currentProduct.flatMap(CartServiceUtils::currentUnitPrice)
                .map(CartServiceUtils::toMoneyScale);
        BigDecimal originalPrice = currentProduct
                .filter(availableProduct -> availableProduct.salePrice() != null)
                .map(Product::basePrice)
                .orElse(null);
        BigDecimal lineTotal = unitPrice.map(price -> price.multiply(BigDecimal.valueOf(item.quantity()))).orElse(null);
        boolean priceChanged = unitPrice
                .flatMap(price -> Optional.ofNullable(item.unitPriceSnapshot())
                        .map(snapshot -> toMoneyScale(snapshot).compareTo(price) != 0))
                .orElse(false);
        return new PricedCartItem(item, product, variant, unitPrice.orElse(null), originalPrice, lineTotal,
                statusOf(item, product, variant), priceChanged);
    }

    public static CartItemStatus statusOf(CartItem item, Product product, ProductVariant variant) {
        boolean productAvailable = Optional.ofNullable(product).filter(Product::active).isPresent();
        Optional<ProductVariant> activeVariant = Optional.ofNullable(variant).filter(CartServiceUtils::isActiveVariant);
        int stock = activeVariant.map(CartServiceUtils::stockOf).orElse(0);
        if (!productAvailable || stock <= 0) {
            return CartItemStatus.UNAVAILABLE;
        }
        if (stock < item.quantity()) {
            return CartItemStatus.INSUFFICIENT_STOCK;
        }
        return CartItemStatus.AVAILABLE;
    }

    public static Optional<BigDecimal> currentUnitPrice(Product product) {
        return Optional.ofNullable(product.salePrice()).or(() -> Optional.ofNullable(product.basePrice()));
    }

    public static BigDecimal toMoneyScale(BigDecimal amount) {
        return amount.setScale(MoneyConstants.AMOUNT_FRACTION_DIGITS, MoneyConstants.AMOUNT_ROUNDING_MODE);
    }

    public static boolean isActiveVariant(ProductVariant variant) {
        return Boolean.TRUE.equals(variant.active());
    }

    public static int stockOf(ProductVariant variant) {
        return Optional.ofNullable(variant.stockQuantity()).orElse(0);
    }
}
