package com.clothingstore.clothing_store_be.model.cart;

import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;

import java.math.BigDecimal;

public record PricedCartItem(
        CartItem item,
        Product product,
        ProductVariant variant,
        BigDecimal unitPrice,
        BigDecimal originalPrice,
        BigDecimal lineTotal,
        CartItemStatus status,
        boolean priceChanged) {
}
