package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URI;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorMessages {

    public static final URI DEFAULT_PROBLEM_TYPE = URI.create("about:blank");
    public static final String ERROR_PATH_PLACEHOLDER = "${server.error.path}";
    public static final String MISSING_ERROR_PROBLEM_FORMAT = "No problem definition is configured for error code %s";
    public static final String CODE_PROPERTY = "code";
    public static final String ERRORS_PROPERTY = "errors";
    public static final String RESOURCE_NOT_FOUND_TITLE = "Resource not found";
    public static final String CATEGORY_NOT_FOUND_DETAIL = "Category not found.";
    public static final String PRODUCT_NOT_FOUND_DETAIL = "Product not found.";
    public static final String CART_NOT_FOUND_DETAIL = "Cart not found.";
    public static final String CART_ITEM_NOT_FOUND_DETAIL = "Cart item not found.";
    public static final String CONFLICT_TITLE = "Conflict";
    public static final String VARIANT_UNAVAILABLE_DETAIL = "The selected product variant is not available.";
    public static final String INSUFFICIENT_STOCK_DETAIL = "Not enough stock is available for the requested quantity.";
    public static final String CART_ITEM_LIMIT_EXCEEDED_DETAIL =
            "The requested quantity exceeds the maximum allowed for one cart item.";
    public static final String CART_CONFLICT_DETAIL = "The cart was modified concurrently. Please reload it and try again.";
    public static final String MISSING_HEADER_MESSAGE = "must be provided";
    public static final String MALFORMED_REQUEST_BODY_MESSAGE = "request body is missing or malformed";
    public static final String MISSING_PRODUCT_CATEGORY_FORMAT =
            "Data integrity error: product %s references a missing category";
}
