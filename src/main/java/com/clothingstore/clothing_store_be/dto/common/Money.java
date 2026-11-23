package com.clothingstore.clothing_store_be.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

public record Money(
        @Schema(example = "49.90", description = "Decimal string with exactly 2 fraction digits") String amount,
        @Schema(example = "ILS", description = "ISO 4217 currency code") String currency) {
}
