package com.clothingstore.clothing_store_be.model.cart;

import java.math.BigDecimal;
import java.util.List;

public record PricedCart(
        String id,
        List<PricedCartItem> items,
        int itemCount,
        BigDecimal subtotal,
        int maxItemQuantity) {
}
