package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorMessages {

    public static final String CODE_PROPERTY = "code";
    public static final String ERRORS_PROPERTY = "errors";
    public static final String RESOURCE_NOT_FOUND_TITLE = "Resource not found";
    public static final String CATEGORY_NOT_FOUND_DETAIL = "Category not found.";
    public static final String PRODUCT_NOT_FOUND_DETAIL = "Product not found.";
    public static final String VALIDATION_FAILED_TITLE = "Validation failed";
    public static final String VALIDATION_FAILED_DETAIL = "One or more request parameters are invalid.";
    public static final String INTERNAL_ERROR_TITLE = "Internal server error";
    public static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred. Please try again later.";
    public static final String MISSING_PRODUCT_CATEGORY_FORMAT =
            "Data integrity error: product %s references a missing category";
}
