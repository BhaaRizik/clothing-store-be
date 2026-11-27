package com.clothingstore.clothing_store_be.exception.common;

import lombok.Getter;

import java.util.Map;

@Getter
public abstract class ConflictException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> problemProperties;

    protected ConflictException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    protected ConflictException(ErrorCode code, String message, Map<String, Object> problemProperties) {
        super(message);
        this.code = code;
        this.problemProperties = problemProperties;
    }
}
