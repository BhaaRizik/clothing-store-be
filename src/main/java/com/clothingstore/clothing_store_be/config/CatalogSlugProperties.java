package com.clothingstore.clothing_store_be.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.catalog.slug")
public record CatalogSlugProperties(@Positive int maxLength) {
}
