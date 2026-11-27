package com.clothingstore.clothing_store_be.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.cart")
public record CartProperties(
        @Positive int maxItemQuantity,
        @NotNull Duration inactiveCartExpiry) {
}
