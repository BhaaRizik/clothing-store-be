package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.constants.LogMessages;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import com.clothingstore.clothing_store_be.dto.common.FieldErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle(ErrorMessages.RESOURCE_NOT_FOUND_TITLE);
        problem.setProperty(ErrorMessages.CODE_PROPERTY, exception.getCode().name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.add(new FieldErrorResponse(lastNodeName(violation.getPropertyPath()), violation.getMessage()));
        }
        return validationProblem(errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
        log.error(LogMessages.UNHANDLED_REQUEST_EXCEPTION, exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorMessages.INTERNAL_ERROR_DETAIL);
        problem.setTitle(ErrorMessages.INTERNAL_ERROR_TITLE);
        problem.setProperty(ErrorMessages.CODE_PROPERTY, ErrorCode.INTERNAL_ERROR.name());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new FieldErrorResponse(parameterName, error.getDefaultMessage()));
            }
        });
        return asObjectResponse(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.add(new FieldErrorResponse(error.getField(), error.getDefaultMessage()));
        }
        return asObjectResponse(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : exception.getPropertyName();
        String expectedTypeName = Optional.ofNullable(exception.getRequiredType())
                .map(requiredType -> requiredType.getSimpleName().toLowerCase(Locale.ROOT))
                .orElse(ValidationConstants.UNKNOWN_TYPE_NAME);
        FieldErrorResponse error = new FieldErrorResponse(field,
                ValidationConstants.INVALID_TYPE_MESSAGE_PREFIX + expectedTypeName);
        return asObjectResponse(validationProblem(List.of(error)));
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<FieldErrorResponse> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ErrorMessages.VALIDATION_FAILED_DETAIL);
        problem.setTitle(ErrorMessages.VALIDATION_FAILED_TITLE);
        problem.setProperty(ErrorMessages.CODE_PROPERTY, ErrorCode.VALIDATION_ERROR.name());
        problem.setProperty(ErrorMessages.ERRORS_PROPERTY, errors.stream()
                .sorted(Comparator.comparing(FieldErrorResponse::field, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private static String lastNodeName(Path propertyPath) {
        String lastName = null;
        for (Path.Node node : propertyPath) {
            lastName = node.getName();
        }
        return lastName;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResponseEntity<Object> asObjectResponse(ResponseEntity<ProblemDetail> response) {
        return (ResponseEntity) response;
    }
}
