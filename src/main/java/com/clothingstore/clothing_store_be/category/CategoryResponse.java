package com.clothingstore.clothing_store_be.category;

/** Public representation of an active category. */
public record CategoryResponse(
        String id,
        String slug,
        String name,
        String description,
        String imageUrl,
        int displayOrder) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.id(), category.slug(), category.name(),
                category.description(), category.imageUrl(), category.displayOrder());
    }
}
