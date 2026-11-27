package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CartConstants {

    public static final String CART_PATH = "/api/v1/cart";
    public static final String CART_CORS_PATH_PATTERN = CART_PATH + "/**";
    public static final String CART_ITEMS_PATH = "/items";
    public static final String CART_ITEM_PATH = "/items/{variantId}";
    public static final String CART_ID_HEADER = "X-Cart-Id";
    public static final String CART_ID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    public static final Pattern CART_ID_PATTERN = Pattern.compile(CART_ID_REGEX);
    public static final String UPDATED_AT_FIELD = "updatedAt";
    public static final String INACTIVE_CART_EXPIRY_INDEX = "ttl_carts_updated_at";
    public static final String MAX_ITEM_QUANTITY_PROPERTY = "maxItemQuantity";
}
