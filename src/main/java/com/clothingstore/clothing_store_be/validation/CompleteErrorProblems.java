package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CompleteErrorProblemsValidator.class)
public @interface CompleteErrorProblems {

    String message() default ValidationConstants.COMPLETE_ERROR_PROBLEMS_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
