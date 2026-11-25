package com.clothingstore.clothing_store_be.dto.product;

import com.clothingstore.clothing_store_be.dto.category.CategoryReferenceResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;

import java.util.List;

public record ProductDetailsResponse(
        String id,
        String slug,
        String name,
        String description,
        String brand,
        List<ProductImageResponse> images,
        Money price,
        Money salePrice,
        CategoryReferenceResponse category,
        List<ProductVariantResponse> variants) {
}
