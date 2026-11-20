package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.common.error.ErrorCode;
import com.clothingstore.clothing_store_be.common.error.ResourceNotFoundException;

/** Product does not exist or is inactive. */
public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException() {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Product not found.");
    }
}
