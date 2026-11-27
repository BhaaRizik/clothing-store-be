package com.clothingstore.clothing_store_be.dto.cart;

import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.dto.product.ProductColorResponse;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CartItemResponse(
        String variantId,
        String productId,
        @Schema(description = "Null when the product no longer exists") String productSlug,
        @Schema(description = "Null when the product no longer exists") String productName,
        @Schema(description = "Image matching the variant color, or null") ImageResponse image,
        @Schema(description = "Null when the variant has no size or no longer exists") String size,
        @Schema(description = "Null when the variant has no color or no longer exists") ProductColorResponse color,
        int quantity,
        @Schema(description = "Current catalog unit price; null when the product no longer exists") Money unitPrice,
        @Schema(description = "Regular price when the item is on sale, otherwise null") Money originalPrice,
        @Schema(description = "unitPrice multiplied by quantity; null when the product no longer exists") Money lineTotal,
        CartItemStatus status,
        @Schema(description = "True when the unit price differs from the price at the time it was added or changed")
        boolean priceChanged) {
}
