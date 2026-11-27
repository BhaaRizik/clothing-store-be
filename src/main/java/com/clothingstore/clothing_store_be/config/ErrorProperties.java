package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import com.clothingstore.clothing_store_be.validation.CompleteErrorProblems;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@CompleteErrorProblems
@ConfigurationProperties("app.errors")
public record ErrorProperties(
        @NotNull Map<HttpStatus, ErrorCode> statusCodes,
        @NotNull ErrorCode clientErrorCode,
        @NotNull ErrorCode serverErrorCode,
        @NotEmpty Map<ErrorCode, @Valid ErrorProblemDefinition> problems) {
}
