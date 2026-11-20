package com.clothingstore.clothing_store_be.common.error;

/** One entry of the {@code errors} array in a {@code VALIDATION_ERROR} problem detail. */
public record FieldErrorResponse(String field, String message) {
}
