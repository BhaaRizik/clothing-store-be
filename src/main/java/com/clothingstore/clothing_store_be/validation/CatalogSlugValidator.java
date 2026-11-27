package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CatalogSlugValidator {

    private final CatalogSlugProperties catalogSlugProperties;

    public boolean isValidCatalogSlug(String slug) {
        return Optional.ofNullable(slug)
                .filter(candidate -> candidate.length() <= catalogSlugProperties.maxLength())
                .map(candidate -> ValidationConstants.CATALOG_SLUG_PATTERN.matcher(candidate).matches())
                .orElse(false);
    }
}
