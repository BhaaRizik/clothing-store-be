package com.clothingstore.clothing_store_be.exception.cart;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.exception.common.ConflictException;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;

public class CartConflictException extends ConflictException {

    public CartConflictException(Throwable cause) {
        super(ErrorCode.CART_CONFLICT, ErrorMessages.CART_CONFLICT_DETAIL);
        initCause(cause);
    }
}
