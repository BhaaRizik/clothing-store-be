package com.clothingstore.clothing_store_be.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Store-wide settings bound from {@code store.*}.
 *
 * @param currency ISO 4217 currency code attached to every price (env {@code STORE_CURRENCY})
 */
@Validated
@ConfigurationProperties("store")
public record StoreProperties(@Pattern(regexp = "[A-Z]{3}") String currency) {
}
