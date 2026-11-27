package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CatalogSlugConstraintValidator implements ConstraintValidator<CatalogSlug, String> {

    private final CatalogSlugProperties catalogSlugProperties;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        int configuredMaxLength = catalogSlugProperties.maxLength();
        boolean wellFormed = Optional.ofNullable(value)
                .map(slug -> slug.length() <= configuredMaxLength
                        && ValidationConstants.CATALOG_SLUG_PATTERN.matcher(slug).matches())
                .orElse(true);
        if (!wellFormed) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            ValidationConstants.CATALOG_SLUG_MESSAGE_FORMAT.formatted(configuredMaxLength))
                    .addConstraintViolation();
        }
        return wellFormed;
    }
}
