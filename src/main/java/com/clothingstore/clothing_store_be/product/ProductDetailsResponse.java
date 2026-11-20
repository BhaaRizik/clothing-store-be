package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.common.Money;

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
        CategoryRef category) {

    /** Minimal reference to the product's category. */
    public record CategoryRef(String slug, String name) {
    }
}
