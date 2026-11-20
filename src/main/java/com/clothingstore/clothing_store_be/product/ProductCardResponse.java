package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.common.Money;

/** Compact product representation for listing cards. {@code primaryImage} is the first image or null. */
public record ProductCardResponse(
        String id,
        String slug,
        String name,
        ImageResponse primaryImage,
        Money price,
        Money salePrice) {
}
