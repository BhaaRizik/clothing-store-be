package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConfiguredCartItemQuantityValidator.class)
public @interface ConfiguredCartItemQuantity {

    String message() default ValidationConstants.CART_ITEM_QUANTITY_RANGE_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
