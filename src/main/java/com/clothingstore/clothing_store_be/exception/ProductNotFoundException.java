package com.clothingstore.clothing_store_be.exception;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;

public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException() {
        super(ErrorCode.PRODUCT_NOT_FOUND, ErrorMessages.PRODUCT_NOT_FOUND_DETAIL);
    }
}
