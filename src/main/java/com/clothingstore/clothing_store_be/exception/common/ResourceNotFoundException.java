package com.clothingstore.clothing_store_be.exception.common;

import lombok.Getter;

@Getter
public abstract class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode code;

    protected ResourceNotFoundException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
