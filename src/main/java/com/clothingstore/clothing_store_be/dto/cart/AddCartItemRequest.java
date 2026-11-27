package com.clothingstore.clothing_store_be.dto.cart;

import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import com.clothingstore.clothing_store_be.validation.ConfiguredCartItemQuantity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddCartItemRequest(
        @Schema(example = "6650a1b2c3d4e5f6a7b8c9d0", description = "Product variant id, 24 hexadecimal characters")
        @NotNull @Pattern(regexp = ValidationConstants.OBJECT_ID_REGEX, message = ValidationConstants.VARIANT_ID_FORMAT_MESSAGE)
        String variantId,
        @Schema(example = "1", description = "Units to add, 1..${app.cart.max-item-quantity} per cart line")
        @NotNull @ConfiguredCartItemQuantity Integer quantity) {
}
