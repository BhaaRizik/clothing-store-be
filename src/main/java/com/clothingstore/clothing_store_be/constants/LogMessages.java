package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMessages {

    public static final String APPLICATION_STARTED = "Clothing Store backend started successfully with active profiles: {}";
    public static final String CART_CREATED = "Guest cart created";
    public static final String CART_CONCURRENT_MODIFICATION = "Concurrent cart modification rejected";
    public static final String CART_EXPIRY_INDEX_CREATED = "Cart expiry index created with expiry {}";
    public static final String CART_EXPIRY_INDEX_UPDATED = "Cart expiry index updated to expiry {}";
    public static final String UNHANDLED_REQUEST_EXCEPTION = "Unhandled exception while processing request";
}
