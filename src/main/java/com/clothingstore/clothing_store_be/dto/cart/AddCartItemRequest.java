package com.clothingstore.clothing_store_be.dto.cart;

import com.clothingstore.clothing_store_be.validation.ConfiguredCartItemQuantity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @Schema(example = "6650a1b2c3d4e5f6a7b8c9d0", description = "Product variant id") @NotBlank String variantId,
        @Schema(example = "1", description = "Units to add, 1..${app.cart.max-item-quantity} per cart line")
        @NotNull @ConfiguredCartItemQuantity Integer quantity) {
}
