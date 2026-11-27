package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.Optional;

public class ProblemDetailFactory {

    public ProblemDetail createProblemForStatus(HttpStatusCode statusCode) {
        return switch (Optional.ofNullable(HttpStatus.resolve(statusCode.value()))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR)) {
            case NOT_FOUND -> createCodedProblem(statusCode, ErrorMessages.ROUTE_NOT_FOUND_TITLE,
                    ErrorMessages.ROUTE_NOT_FOUND_DETAIL, ErrorCode.ROUTE_NOT_FOUND);
            case METHOD_NOT_ALLOWED -> createCodedProblem(statusCode, ErrorMessages.METHOD_NOT_ALLOWED_TITLE,
                    ErrorMessages.METHOD_NOT_ALLOWED_DETAIL, ErrorCode.METHOD_NOT_ALLOWED);
            case UNSUPPORTED_MEDIA_TYPE -> createCodedProblem(statusCode, ErrorMessages.UNSUPPORTED_MEDIA_TYPE_TITLE,
                    ErrorMessages.UNSUPPORTED_MEDIA_TYPE_DETAIL, ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            case NOT_ACCEPTABLE -> createCodedProblem(statusCode, ErrorMessages.NOT_ACCEPTABLE_TITLE,
                    ErrorMessages.NOT_ACCEPTABLE_DETAIL, ErrorCode.NOT_ACCEPTABLE);
            case SERVICE_UNAVAILABLE -> createServiceUnavailableProblem();
            default -> statusCode.is4xxClientError()
                    ? createClientErrorProblem(statusCode)
                    : createInternalErrorProblem(statusCode);
        };
    }

    public ProblemDetail createServiceUnavailableProblem() {
        return createCodedProblem(HttpStatus.SERVICE_UNAVAILABLE, ErrorMessages.SERVICE_UNAVAILABLE_TITLE,
                ErrorMessages.SERVICE_UNAVAILABLE_DETAIL, ErrorCode.SERVICE_UNAVAILABLE);
    }

    public ProblemDetail createInternalErrorProblem(HttpStatusCode statusCode) {
        return createCodedProblem(statusCode, ErrorMessages.INTERNAL_ERROR_TITLE, ErrorMessages.INTERNAL_ERROR_DETAIL,
                ErrorCode.INTERNAL_ERROR);
    }

    public ProblemDetail createValidationProblem(List<?> errors) {
        ProblemDetail problem = createCodedProblem(HttpStatus.BAD_REQUEST, ErrorMessages.VALIDATION_FAILED_TITLE,
                ErrorMessages.VALIDATION_FAILED_DETAIL, ErrorCode.VALIDATION_ERROR);
        problem.setProperty(ErrorMessages.ERRORS_PROPERTY, errors);
        return problem;
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

    private ProblemDetail createClientErrorProblem(HttpStatusCode statusCode) {
        ProblemDetail problem = createCodedProblem(statusCode, ErrorMessages.VALIDATION_FAILED_TITLE,
                ErrorMessages.VALIDATION_FAILED_DETAIL, ErrorCode.VALIDATION_ERROR);
        problem.setProperty(ErrorMessages.ERRORS_PROPERTY, List.of());
        return problem;
    }
}
