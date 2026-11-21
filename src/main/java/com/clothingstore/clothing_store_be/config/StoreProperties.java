package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("store")
public record StoreProperties(@Pattern(regexp = ValidationConstants.CURRENCY_CODE_PATTERN) String currency) {
}
