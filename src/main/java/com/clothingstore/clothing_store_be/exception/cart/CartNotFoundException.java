package com.clothingstore.clothing_store_be.exception.cart;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import com.clothingstore.clothing_store_be.exception.common.ResourceNotFoundException;

public class CartNotFoundException extends ResourceNotFoundException {

    public CartNotFoundException() {
        super(ErrorCode.CART_NOT_FOUND, ErrorMessages.CART_NOT_FOUND_DETAIL);
    }
}
