package com.clothingstore.clothing_store_be.exception.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

    private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

    @ParameterizedTest
    @CsvSource({
            "400, VALIDATION_ERROR",
            "404, ROUTE_NOT_FOUND",
            "405, METHOD_NOT_ALLOWED",
            "406, NOT_ACCEPTABLE",
            "413, VALIDATION_ERROR",
            "415, UNSUPPORTED_MEDIA_TYPE",
            "499, VALIDATION_ERROR",
            "500, INTERNAL_ERROR",
            "502, INTERNAL_ERROR",
            "503, SERVICE_UNAVAILABLE",
            "599, INTERNAL_ERROR"
    })
    void mapsEveryStatusToAStableCodeAndKeepsTheStatus(int status, String expectedCode) {
        ProblemDetail problem = problemDetailFactory.createProblemForStatus(HttpStatusCode.valueOf(status));

        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getType()).hasToString("about:blank");
        assertThat(problem.getTitle()).isNotBlank();
        assertThat(problem.getDetail()).isNotBlank();
        assertThat(problem.getProperties()).containsEntry("code", expectedCode);
    }
}
