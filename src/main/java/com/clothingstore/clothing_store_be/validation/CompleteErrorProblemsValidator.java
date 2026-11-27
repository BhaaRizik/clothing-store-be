package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.ErrorProblemDefinition;
import com.clothingstore.clothing_store_be.config.ErrorProperties;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CompleteErrorProblemsValidator implements ConstraintValidator<CompleteErrorProblems, ErrorProperties> {

    @Override
    public boolean isValid(ErrorProperties errorProperties, ConstraintValidatorContext context) {
        List<ErrorCode> undefinedCodes = Optional.ofNullable(errorProperties)
                .map(CompleteErrorProblemsValidator::findCodesWithoutProblemDefinition)
                .orElseGet(List::of);
        if (!undefinedCodes.isEmpty()) {
            context.disableDefaultConstraintViolation();
            undefinedCodes.forEach(code -> context.buildConstraintViolationWithTemplate(
                            ValidationConstants.MISSING_ERROR_PROBLEM_MESSAGE_FORMAT.formatted(code.name()))
                    .addPropertyNode(ValidationConstants.ERROR_PROBLEMS_PROPERTY)
                    .addConstraintViolation());
        }
        return undefinedCodes.isEmpty();
    }

    private static List<ErrorCode> findCodesWithoutProblemDefinition(ErrorProperties errorProperties) {
        Map<ErrorCode, ErrorProblemDefinition> problems = Optional.ofNullable(errorProperties.problems())
                .orElseGet(Map::of);
        return Stream.concat(
                        Optional.ofNullable(errorProperties.statusCodes()).map(Map::values).stream()
                                .flatMap(Collection::stream),
                        Stream.of(errorProperties.clientErrorCode(), errorProperties.serverErrorCode()))
                .filter(Objects::nonNull)
                .distinct()
                .filter(code -> !hasTitleAndDetail(problems.get(code)))
                .toList();
    }

    private static boolean hasTitleAndDetail(ErrorProblemDefinition definition) {
        return Optional.ofNullable(definition)
                .map(problem -> StringUtils.hasText(problem.title()) && StringUtils.hasText(problem.detail()))
                .orElse(false);
    }
}
