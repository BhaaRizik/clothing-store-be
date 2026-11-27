package com.clothingstore.clothing_store_be.config;

import jakarta.validation.constraints.NotBlank;

public record ErrorProblemDefinition(
        @NotBlank String title,
        @NotBlank String detail) {
}
