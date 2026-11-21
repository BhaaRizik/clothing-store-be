package com.clothingstore.clothing_store_be.dto;

public record ProductCardResponse(
        String id,
        String slug,
        String name,
        ImageResponse primaryImage,
        Money price,
        Money salePrice) {
}
