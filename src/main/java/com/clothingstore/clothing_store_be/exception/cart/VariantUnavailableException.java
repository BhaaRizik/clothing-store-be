package com.clothingstore.clothing_store_be.exception.cart;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import com.clothingstore.clothing_store_be.exception.common.ConflictException;

public class VariantUnavailableException extends ConflictException {

    public VariantUnavailableException() {
        super(ErrorCode.VARIANT_UNAVAILABLE, ErrorMessages.VARIANT_UNAVAILABLE_DETAIL);
    }
}
