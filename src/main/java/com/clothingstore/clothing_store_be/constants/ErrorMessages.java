package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URI;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorMessages {

    public static final URI DEFAULT_PROBLEM_TYPE = URI.create("about:blank");
    public static final String ERROR_PATH = "${server.error.path:${error.path:/error}}";
    public static final String CODE_PROPERTY = "code";
    public static final String ERRORS_PROPERTY = "errors";
    public static final String RESOURCE_NOT_FOUND_TITLE = "Resource not found";
    public static final String CATEGORY_NOT_FOUND_DETAIL = "Category not found.";
    public static final String PRODUCT_NOT_FOUND_DETAIL = "Product not found.";
    public static final String VALIDATION_FAILED_TITLE = "Validation failed";
    public static final String VALIDATION_FAILED_DETAIL = "One or more request parameters are invalid.";
    public static final String INTERNAL_ERROR_TITLE = "Internal server error";
    public static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred. Please try again later.";
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
    public static final String ROUTE_NOT_FOUND_TITLE = "Route not found";
    public static final String ROUTE_NOT_FOUND_DETAIL = "The requested endpoint does not exist.";
    public static final String METHOD_NOT_ALLOWED_TITLE = "Method not allowed";
    public static final String METHOD_NOT_ALLOWED_DETAIL = "The HTTP method is not supported for this endpoint.";
    public static final String UNSUPPORTED_MEDIA_TYPE_TITLE = "Unsupported media type";
    public static final String UNSUPPORTED_MEDIA_TYPE_DETAIL = "The request content type is not supported.";
    public static final String NOT_ACCEPTABLE_TITLE = "Not acceptable";
    public static final String NOT_ACCEPTABLE_DETAIL = "The requested response media type is not supported.";
    public static final String SERVICE_UNAVAILABLE_TITLE = "Service unavailable";
    public static final String SERVICE_UNAVAILABLE_DETAIL = "The service is temporarily unavailable. Please try again later.";
}
