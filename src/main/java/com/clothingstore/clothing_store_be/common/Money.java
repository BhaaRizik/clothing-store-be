package com.clothingstore.clothing_store_be.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Monetary amount as exposed by the API: a decimal string with exactly two fraction digits
 * plus an ISO 4217 currency code, e.g. {@code {"amount": "49.90", "currency": "ILS"}}.
 */
public record Money(
        @Schema(example = "49.90", description = "Decimal string with exactly 2 fraction digits") String amount,
        @Schema(example = "ILS", description = "ISO 4217 currency code") String currency) {
}
