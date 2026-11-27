package com.clothingstore.clothing_store_be.dto.cart;

import com.clothingstore.clothing_store_be.dto.common.Money;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CartResponse(
        @Schema(description = "Cart id; send it back in the X-Cart-Id header") String id,
        @Schema(description = "Cart lines ordered by the time they were added") List<CartItemResponse> items,
        @Schema(description = "Sum of quantities over all lines") int itemCount,
        @Schema(description = "Sum of lineTotal over lines that are not UNAVAILABLE") Money subtotal,
        @Schema(description = "Maximum quantity of one cart line") int maxItemQuantity) {
}
