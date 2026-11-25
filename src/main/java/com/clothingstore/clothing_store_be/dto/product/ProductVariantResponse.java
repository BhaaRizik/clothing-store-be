package com.clothingstore.clothing_store_be.dto.product;

public record ProductVariantResponse(
        String id,
        String size,
        ProductColorResponse color,
        boolean available) {
}
