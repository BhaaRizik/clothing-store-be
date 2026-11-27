package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.CartProperties;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ConfiguredCartItemQuantityValidator implements ConstraintValidator<ConfiguredCartItemQuantity, Integer> {

    private final CartProperties cartProperties;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        int configuredMaximum = cartProperties.maxItemQuantity();
        boolean withinRange = Optional.ofNullable(value)
                .map(quantity -> quantity >= ValidationConstants.MIN_CART_ITEM_QUANTITY && quantity <= configuredMaximum)
                .orElse(true);
        if (!withinRange) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ValidationConstants.CART_ITEM_QUANTITY_RANGE_MESSAGE_FORMAT
                            .formatted(ValidationConstants.MIN_CART_ITEM_QUANTITY, configuredMaximum))
                    .addConstraintViolation();
        }
        return withinRange;
    }
}
