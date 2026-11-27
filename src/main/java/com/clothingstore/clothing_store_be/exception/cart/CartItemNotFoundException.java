package com.clothingstore.clothing_store_be.exception.cart;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import com.clothingstore.clothing_store_be.exception.common.ResourceNotFoundException;

public class CartItemNotFoundException extends ResourceNotFoundException {

    public CartItemNotFoundException() {
        super(ErrorCode.CART_ITEM_NOT_FOUND, ErrorMessages.CART_ITEM_NOT_FOUND_DETAIL);
    }
}
