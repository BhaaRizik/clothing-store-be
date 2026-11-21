package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.CatalogPaginationProperties;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class MaxConfiguredPageValidator implements ConstraintValidator<MaxConfiguredPage, Integer> {

    private final CatalogPaginationProperties catalogPaginationProperties;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        int configuredMaximum = catalogPaginationProperties.maxPage();
        boolean withinLimit = Optional.ofNullable(value).map(number -> number <= configuredMaximum).orElse(true);
        if (!withinLimit) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            ValidationConstants.MAX_VALUE_MESSAGE_FORMAT.formatted(configuredMaximum))
                    .addConstraintViolation();
        }
        return withinLimit;
    }
}
