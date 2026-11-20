package com.clothingstore.clothing_store_be.common.error;

/** Base class for "resource does not exist (or is not publicly visible)" failures, mapped to 404. */
public abstract class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode code;

    protected ResourceNotFoundException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
