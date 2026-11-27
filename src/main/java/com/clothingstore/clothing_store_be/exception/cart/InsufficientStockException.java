package com.clothingstore.clothing_store_be.exception.cart;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import com.clothingstore.clothing_store_be.exception.common.ConflictException;

public class InsufficientStockException extends ConflictException {

    public InsufficientStockException() {
        super(ErrorCode.INSUFFICIENT_STOCK, ErrorMessages.INSUFFICIENT_STOCK_DETAIL);
    }
}
