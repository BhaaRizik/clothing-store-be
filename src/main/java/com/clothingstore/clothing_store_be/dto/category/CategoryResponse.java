package com.clothingstore.clothing_store_be.dto.category;

public record CategoryResponse(
        String id,
        String slug,
        String name,
        String description,
        String imageUrl,
        int displayOrder) {
}
