package com.clothingstore.clothing_store_be.exception;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException() {
        super(ErrorCode.CATEGORY_NOT_FOUND, ErrorMessages.CATEGORY_NOT_FOUND_DETAIL);
    }
}
