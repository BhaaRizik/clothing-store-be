package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.config.ApplicationYamlErrorPropertiesLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

    private final ProblemDetailFactory problemDetailFactory =
            new ProblemDetailFactory(new ApplicationYamlErrorPropertiesLoader().loadErrorProperties());

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "400 | VALIDATION_ERROR       | Validation failed      | One or more request parameters are invalid.",
            "404 | ROUTE_NOT_FOUND        | Route not found        | The requested endpoint does not exist.",
            "405 | METHOD_NOT_ALLOWED     | Method not allowed     | The HTTP method is not supported for this endpoint.",
            "406 | NOT_ACCEPTABLE         | Not acceptable         | The requested response media type is not supported.",
            "413 | VALIDATION_ERROR       | Validation failed      | One or more request parameters are invalid.",
            "415 | UNSUPPORTED_MEDIA_TYPE | Unsupported media type | The request content type is not supported.",
            "499 | VALIDATION_ERROR       | Validation failed      | One or more request parameters are invalid.",
            "500 | INTERNAL_ERROR         | Internal server error  | An unexpected error occurred. Please try again later.",
            "502 | INTERNAL_ERROR         | Internal server error  | An unexpected error occurred. Please try again later.",
            "503 | SERVICE_UNAVAILABLE    | Service unavailable    | The service is temporarily unavailable. Please try again later.",
            "599 | INTERNAL_ERROR         | Internal server error  | An unexpected error occurred. Please try again later."
    })
    void mapsEveryStatusToTheConfiguredProblemAndKeepsTheStatus(int status, String expectedCode, String expectedTitle,
            String expectedDetail) {
        ProblemDetail problem = problemDetailFactory.createProblemForStatus(HttpStatusCode.valueOf(status));

        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getType()).hasToString("about:blank");
        assertThat(problem.getTitle()).isEqualTo(expectedTitle);
        assertThat(problem.getDetail()).isEqualTo(expectedDetail);
        assertThat(problem.getProperties()).containsEntry("code", expectedCode);
    }

    @ParameterizedTest
    @CsvSource({"400", "413", "499"})
    void unmappedClientErrorsCarryAnEmptyErrorsList(int status) {
        ProblemDetail problem = problemDetailFactory.createProblemForStatus(HttpStatusCode.valueOf(status));

        assertThat(problem.getProperties()).containsEntry("errors", List.of());
    }

    @ParameterizedTest
    @CsvSource({"404", "405", "406", "415", "500", "503"})
    void mappedAndServerErrorsCarryOnlyTheCode(int status) {
        ProblemDetail problem = problemDetailFactory.createProblemForStatus(HttpStatusCode.valueOf(status));

        assertThat(problem.getProperties()).containsOnlyKeys("code");
    }

    @Test
    void validationProblemIsA400WithTheGivenErrors() {
        ProblemDetail problem = problemDetailFactory.createValidationProblem(List.of("first"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("errors", List.of("first"));
    }

    @Test
    void serviceUnavailableProblemUsesTheConfiguredDefinition() {
        ProblemDetail problem = problemDetailFactory.createServiceUnavailableProblem();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Service unavailable");
        assertThat(problem.getProperties()).containsEntry("code", "SERVICE_UNAVAILABLE");
    }
}
