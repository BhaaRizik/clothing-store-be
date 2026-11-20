package com.clothingstore.clothing_store_be.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Allowed browser origins for the public API, bound from {@code app.cors.allowed-origins}
 * (environment variable {@code FRONTEND_ORIGINS}, comma-separated).
 * <p>
 * Each entry is trimmed and must be an explicit origin ({@code scheme://host[:port]}): wildcards ({@code *}),
 * missing schemes, paths and trailing slashes fail startup (PROJECT.md section 8: no wildcard CORS).
 */
@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(
        @NotEmpty
        List<@Pattern(regexp = ORIGIN_PATTERN, message = "must be an explicit origin like https://shop.example.com[:port]")
                String> allowedOrigins) {

    static final String ORIGIN_PATTERN = "https?://[A-Za-z0-9.-]+(:\\d{1,5})?";

    public CorsProperties {
        // Trim before validation runs (validation happens on the constructed record).
        allowedOrigins = allowedOrigins == null ? null
                : allowedOrigins.stream().map(origin -> origin == null ? null : origin.trim()).toList();
    }
}
