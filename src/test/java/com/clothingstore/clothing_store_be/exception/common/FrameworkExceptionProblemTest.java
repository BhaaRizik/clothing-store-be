package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.config.ApplicationYamlErrorPropertiesLoader;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkExceptionProblemTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            new ProblemDetailFactory(new ApplicationYamlErrorPropertiesLoader().loadErrorProperties()));

    private ProblemDetail handle(Exception exception, HttpStatus expectedStatus) throws Exception {
        ResponseEntity<Object> response = handler.handleException(exception,
                new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getType()).hasToString("about:blank");
        assertThat(problem.getStatus()).isEqualTo(expectedStatus.value());
        return problem;
    }

    private static void assertProblemText(ProblemDetail problem, String code, String title, String detail) {
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isEqualTo(detail);
        assertThat(problem.getProperties()).containsEntry("code", code);
    }

    private static MethodParameter stringParameter() throws NoSuchMethodException {
        return new MethodParameter(String.class.getMethod("concat", String.class), 0);
    }

    @Test
    void missingRequestParameterReturns400ValidationErrorWithoutParameterName() throws Exception {
        ProblemDetail problem = handle(new MissingServletRequestParameterException("secretParameter", "String"),
                HttpStatus.BAD_REQUEST);

        assertProblemText(problem, "VALIDATION_ERROR", "Validation failed",
                "One or more request parameters are invalid.");
        assertThat(problem.getProperties()).containsEntry("errors", List.of());
        assertThat(problem.toString()).doesNotContain("secretParameter");
    }

    @Test
    void maxUploadSizeExceededReturns413ValidationError() throws Exception {
        ProblemDetail problem = handle(new MaxUploadSizeExceededException(1024), HttpStatus.CONTENT_TOO_LARGE);

        assertProblemText(problem, "VALIDATION_ERROR", "Validation failed",
                "One or more request parameters are invalid.");
        assertThat(problem.toString()).doesNotContain("1024");
    }

    @Test
    void missingPathVariableReturns500InternalErrorWithoutVariableName() throws Exception {
        ProblemDetail problem = handle(new MissingPathVariableException("secretVariable", stringParameter()),
                HttpStatus.INTERNAL_SERVER_ERROR);

        assertProblemText(problem, "INTERNAL_ERROR", "Internal server error",
                "An unexpected error occurred. Please try again later.");
        assertThat(problem.toString()).doesNotContain("secretVariable");
    }

    @Test
    void messageNotWritableReturns500InternalErrorWithoutMessage() throws Exception {
        ProblemDetail problem = handle(new HttpMessageNotWritableException("secret converter failure"),
                HttpStatus.INTERNAL_SERVER_ERROR);

        assertProblemText(problem, "INTERNAL_ERROR", "Internal server error",
                "An unexpected error occurred. Please try again later.");
        assertThat(problem.toString()).doesNotContain("secret converter failure", "HttpMessageNotWritableException");
    }

    @Test
    void asyncRequestTimeoutReturns503ServiceUnavailable() throws Exception {
        ProblemDetail problem = handle(new AsyncRequestTimeoutException(), HttpStatus.SERVICE_UNAVAILABLE);

        assertProblemText(problem, "SERVICE_UNAVAILABLE", "Service unavailable",
                "The service is temporarily unavailable. Please try again later.");
    }
}
