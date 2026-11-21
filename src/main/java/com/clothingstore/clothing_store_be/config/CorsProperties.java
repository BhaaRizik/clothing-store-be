package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(
        @NotEmpty
        List<@Pattern(regexp = ValidationConstants.ORIGIN_PATTERN, message = ValidationConstants.ORIGIN_PATTERN_MESSAGE)
                String> allowedOrigins) {
}
