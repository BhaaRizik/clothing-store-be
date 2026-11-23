package com.clothingstore.clothing_store_be.dto.product;

import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
public record ProductCardResponse(
        String id,
        String slug,
        String name,
        ImageResponse primaryImage,
        Money price,
        Money salePrice) {
}
