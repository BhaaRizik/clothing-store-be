package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationConstants {

    public static final String ORIGIN_PATTERN = "https?://[A-Za-z0-9.-]+(:\\d{1,5})?";
    public static final String ORIGIN_PATTERN_MESSAGE = "must be an explicit origin like https://shop.example.com[:port]";
    public static final String CURRENCY_CODE_PATTERN = "[A-Z]{3}";
    public static final String PRODUCT_SORT_PATTERN = ProductSortParameters.NEWEST + "|" + ProductSortParameters.NAME + "|"
            + ProductSortParameters.PRICE_ASC + "|" + ProductSortParameters.PRICE_DESC;
    public static final String PRODUCT_SORT_MESSAGE = "must be one of: " + ProductSortParameters.NEWEST + ", "
            + ProductSortParameters.NAME + ", " + ProductSortParameters.PRICE_ASC + ", " + ProductSortParameters.PRICE_DESC;
    public static final String CATALOG_SLUG_REGEX = "[a-z0-9]+(-[a-z0-9]+)*";
    public static final Pattern CATALOG_SLUG_PATTERN = Pattern.compile(CATALOG_SLUG_REGEX);
    public static final String CATALOG_SLUG_MESSAGE =
            "must be a lowercase kebab-case slug of at most the configured maximum length";
    public static final String CATALOG_SLUG_MESSAGE_FORMAT =
            "must be a lowercase kebab-case slug of at most %d characters";
    public static final String OBJECT_ID_REGEX = "[0-9a-fA-F]{24}";
    public static final String VARIANT_ID_FORMAT_MESSAGE = "must be a valid product variant id";
    public static final String MAX_VALUE_MESSAGE = "must be less than or equal to the configured maximum";
    public static final String MAX_VALUE_MESSAGE_FORMAT = "must be less than or equal to %d";
    public static final int MIN_CART_ITEM_QUANTITY = 1;
    public static final String CART_ITEM_QUANTITY_RANGE_MESSAGE = "Quantity must be between 1 and the configured maximum.";
    public static final String CART_ITEM_QUANTITY_RANGE_MESSAGE_FORMAT = "Quantity must be between %d and %d.";
    public static final String INVALID_TYPE_MESSAGE_PREFIX = "must be a valid ";
    public static final String UNKNOWN_TYPE_NAME = "value";
    public static final String COMPLETE_ERROR_PROBLEMS_MESSAGE =
            "every configured error code must have a problem definition with a title and detail";
    public static final String MISSING_ERROR_PROBLEM_MESSAGE_FORMAT =
            "error code %s must have a problem definition with a non-blank title and detail";
    public static final String ERROR_PROBLEMS_PROPERTY = "problems";
}
