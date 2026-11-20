package com.clothingstore.clothing_store_be.category;

import com.clothingstore.clothing_store_be.common.error.ErrorCode;
import com.clothingstore.clothing_store_be.common.error.ResourceNotFoundException;

/** Category does not exist or is inactive. */
public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException() {
        super(ErrorCode.CATEGORY_NOT_FOUND, "Category not found.");
    }
}
