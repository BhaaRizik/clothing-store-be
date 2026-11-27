package com.clothingstore.clothing_store_be.dto.cart;

import com.clothingstore.clothing_store_be.validation.ConfiguredCartItemQuantity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @Schema(example = "2", description = "New absolute quantity, 1..${app.cart.max-item-quantity}")
        @NotNull @ConfiguredCartItemQuantity Integer quantity) {
}
