package com.clothingstore.clothing_store_be.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Maps every failure to an RFC 9457 problem detail ({@code application/problem+json}) carrying a
 * stable {@code code} property. Internal messages and stack traces are never returned to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CODE = "code";
    private static final String ERRORS = "errors";

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setProperty(CODE, ex.getCode().name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /** Raised by method validation of a {@code @Validated} controller (query/path parameters). */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(new FieldErrorResponse(lastNodeName(violation.getPropertyPath()), violation.getMessage()));
        }
        return validationProblem(errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal server error");
        problem.setProperty(CODE, ErrorCode.INTERNAL_ERROR.name());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /** Built-in Spring MVC method validation (used when the controller is not proxied by {@code @Validated}). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new FieldErrorResponse(name, error.getDefaultMessage()));
            }
        });
        return asObject(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldErrorResponse(error.getField(), error.getDefaultMessage()));
        }
        return asObject(validationProblem(errors));
    }

    /** Unparseable parameter values, e.g. {@code page=abc} or {@code featured=maybe}. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName().toLowerCase() : "value";
        return asObject(validationProblem(List.of(new FieldErrorResponse(field, "must be a valid " + expected))));
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<FieldErrorResponse> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more request parameters are invalid.");
        problem.setTitle("Validation failed");
        problem.setProperty(CODE, ErrorCode.VALIDATION_ERROR.name());
        problem.setProperty(ERRORS, errors.stream()
                .sorted(Comparator.comparing(FieldErrorResponse::field, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private static String lastNodeName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResponseEntity<Object> asObject(ResponseEntity<ProblemDetail> response) {
        return (ResponseEntity) response;
    }
}
