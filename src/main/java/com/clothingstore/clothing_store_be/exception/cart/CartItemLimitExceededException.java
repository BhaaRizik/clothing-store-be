package com.clothingstore.clothing_store_be.exception.cart;

import com.clothingstore.clothing_store_be.constants.CartConstants;
import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.exception.common.ConflictException;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;

import java.util.Map;

public class CartItemLimitExceededException extends ConflictException {

    public CartItemLimitExceededException(int maxItemQuantity) {
        super(ErrorCode.CART_ITEM_LIMIT_EXCEEDED, ErrorMessages.CART_ITEM_LIMIT_EXCEEDED_DETAIL,
                Map.of(CartConstants.MAX_ITEM_QUANTITY_PROPERTY, maxItemQuantity));
    }
}
