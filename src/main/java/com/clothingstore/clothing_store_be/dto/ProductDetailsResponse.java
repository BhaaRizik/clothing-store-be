package com.clothingstore.clothing_store_be.dto;

import java.util.List;

public record ProductDetailsResponse(
        String id,
        String slug,
        String name,
        String description,
        String brand,
        List<ImageResponse> images,
        Money price,
        Money salePrice,
        CategoryReferenceResponse category) {
}
