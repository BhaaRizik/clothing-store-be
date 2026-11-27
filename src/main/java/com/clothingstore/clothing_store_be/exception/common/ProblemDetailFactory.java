package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.config.ErrorProblemDefinition;
import com.clothingstore.clothing_store_be.config.ErrorProperties;
import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProblemDetailFactory {

    private final ErrorProperties errorProperties;

    public ProblemDetail createProblemForStatus(HttpStatusCode statusCode) {
        return Optional.ofNullable(HttpStatus.resolve(statusCode.value()))
                .map(status -> errorProperties.statusCodes().get(status))
                .map(code -> createConfiguredProblem(statusCode, code))
                .orElseGet(() -> statusCode.is4xxClientError()
                        ? createClientErrorProblem(statusCode, List.of())
                        : createInternalErrorProblem(statusCode));
    }

    public ProblemDetail createServiceUnavailableProblem() {
        return createProblemForStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ProblemDetail createInternalErrorProblem(HttpStatusCode statusCode) {
        return createConfiguredProblem(statusCode, errorProperties.serverErrorCode());
    }

    public ProblemDetail createValidationProblem(List<?> errors) {
        return createClientErrorProblem(HttpStatus.BAD_REQUEST, errors);
    }

    public ProblemDetail createCodedProblem(HttpStatusCode statusCode, String title, String detail, ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusCode, detail);
        problem.setType(ErrorMessages.DEFAULT_PROBLEM_TYPE);
        problem.setTitle(title);
        problem.setProperty(ErrorMessages.CODE_PROPERTY, code.name());
        return problem;
    }

    public boolean hasErrorCode(ProblemDetail problem) {
        return Optional.ofNullable(problem.getProperties())
                .map(properties -> properties.containsKey(ErrorMessages.CODE_PROPERTY))
                .orElse(false);
    }

    private ProblemDetail createClientErrorProblem(HttpStatusCode statusCode, List<?> errors) {
        ProblemDetail problem = createConfiguredProblem(statusCode, errorProperties.clientErrorCode());
        problem.setProperty(ErrorMessages.ERRORS_PROPERTY, errors);
        return problem;
    }

    private ProblemDetail createConfiguredProblem(HttpStatusCode statusCode, ErrorCode code) {
        ErrorProblemDefinition definition = Optional.ofNullable(errorProperties.problems().get(code))
                .orElseThrow(() -> new IllegalStateException(
                        ErrorMessages.MISSING_ERROR_PROBLEM_FORMAT.formatted(code.name())));
        return createCodedProblem(statusCode, definition.title(), definition.detail(), code);
    }
}
