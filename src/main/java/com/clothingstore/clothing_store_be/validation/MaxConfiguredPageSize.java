package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxConfiguredPageSizeValidator.class)
public @interface MaxConfiguredPageSize {

    String message() default ValidationConstants.MAX_VALUE_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
