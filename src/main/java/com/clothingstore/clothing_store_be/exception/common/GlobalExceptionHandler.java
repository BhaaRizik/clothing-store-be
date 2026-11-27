package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.constants.LogMessages;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import com.clothingstore.clothing_store_be.dto.common.FieldErrorResponse;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.util.ClassUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException exception) {
        ProblemDetail problem = problemDetailFactory.createCodedProblem(HttpStatus.NOT_FOUND, ErrorMessages.RESOURCE_NOT_FOUND_TITLE,
                exception.getMessage(), exception.getCode());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException exception) {
        ProblemDetail problem = problemDetailFactory.createCodedProblem(HttpStatus.CONFLICT, ErrorMessages.CONFLICT_TITLE,
                exception.getMessage(), exception.getCode());
        exception.getProblemProperties().forEach(problem::setProperty);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.add(new FieldErrorResponse(lastNodeName(violation.getPropertyPath()), violation.getMessage()));
        }
        return validationProblem(errors);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, MongoTimeoutException.class,
            MongoSocketException.class})
    ResponseEntity<ProblemDetail> handleDataStoreUnavailable(Exception exception) {
        log.error(LogMessages.DATA_STORE_UNAVAILABLE, exception.getClass().getSimpleName(),
                NestedExceptionUtils.getMostSpecificCause(exception).getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problemDetailFactory.createServiceUnavailableProblem());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
        log.error(LogMessages.UNHANDLED_REQUEST_EXCEPTION, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetailFactory.createInternalErrorProblem(HttpStatus.INTERNAL_SERVER_ERROR));
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

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception instanceof MissingRequestHeaderException missingHeader) {
            FieldErrorResponse error = new FieldErrorResponse(missingHeader.getHeaderName(),
                    ErrorMessages.MISSING_HEADER_MESSAGE);
            return asObjectResponse(validationProblem(List.of(error)));
        }
        return super.handleServletRequestBindingException(exception, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Optional<JacksonException> jsonException = Optional.ofNullable(exception.getMostSpecificCause())
                .filter(JacksonException.class::isInstance)
                .map(JacksonException.class::cast);
        String field = jsonException
                .map(JacksonException::getPath)
                .filter(path -> !path.isEmpty())
                .map(path -> path.getLast().getPropertyName())
                .orElse(null);
        String message = jsonException
                .filter(MismatchedInputException.class::isInstance)
                .map(MismatchedInputException.class::cast)
                .map(MismatchedInputException::getTargetType)
                .filter(targetType -> field != null)
                .map(targetType -> ValidationConstants.INVALID_TYPE_MESSAGE_PREFIX
                        + ClassUtils.resolvePrimitiveIfNecessary(targetType).getSimpleName().toLowerCase(Locale.ROOT))
                .orElse(ErrorMessages.MALFORMED_REQUEST_BODY_MESSAGE);
        FieldErrorResponse error = new FieldErrorResponse(field, message);
        return asObjectResponse(validationProblem(List.of(error)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error(LogMessages.FRAMEWORK_SERVER_ERROR, exception);
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        ProblemDetail problem = Optional.ofNullable(body)
                .filter(ProblemDetail.class::isInstance)
                .map(ProblemDetail.class::cast)
                .filter(problemDetailFactory::hasErrorCode)
                .orElseGet(() -> problemDetailFactory.createProblemForStatus(statusCode));
        return super.createResponseEntity(problem, headers, statusCode, request);
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<FieldErrorResponse> errors) {
        List<FieldErrorResponse> sortedErrors = errors.stream()
                .sorted(Comparator.comparing(FieldErrorResponse::field, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(problemDetailFactory.createValidationProblem(sortedErrors));
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
