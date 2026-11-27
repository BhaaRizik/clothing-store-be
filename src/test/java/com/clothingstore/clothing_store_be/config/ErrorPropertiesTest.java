package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.exception.common.ErrorCode;
import com.clothingstore.clothing_store_be.exception.common.ProblemDetailFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorPropertiesTest {

    private static final List<String> COMPLETE_ERROR_PROPERTIES = List.of(
            "app.errors.status-codes[NOT_FOUND]=ROUTE_NOT_FOUND",
            "app.errors.client-error-code=VALIDATION_ERROR",
            "app.errors.server-error-code=INTERNAL_ERROR",
            "app.errors.problems[ROUTE_NOT_FOUND].title=Route not found",
            "app.errors.problems[ROUTE_NOT_FOUND].detail=The requested endpoint does not exist.",
            "app.errors.problems[VALIDATION_ERROR].title=Validation failed",
            "app.errors.problems[VALIDATION_ERROR].detail=One or more request parameters are invalid.",
            "app.errors.problems[INTERNAL_ERROR].title=Internal server error",
            "app.errors.problems[INTERNAL_ERROR].detail=An unexpected error occurred.");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ErrorPropertiesTestConfiguration.class);

    private static String[] completePropertiesWithout(String excludedPrefix, String... extraProperties) {
        List<String> properties = new ArrayList<>(COMPLETE_ERROR_PROPERTIES.stream()
                .filter(property -> !property.startsWith(excludedPrefix))
                .toList());
        properties.addAll(List.of(extraProperties));
        return properties.toArray(String[]::new);
    }

    @Test
    void applicationYamlDefinesEveryReferencedErrorCode() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ErrorProperties errorProperties = context.getBean(ErrorProperties.class);
                    assertThat(errorProperties.statusCodes())
                            .containsEntry(HttpStatus.NOT_FOUND, ErrorCode.ROUTE_NOT_FOUND)
                            .containsEntry(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED)
                            .containsEntry(HttpStatus.NOT_ACCEPTABLE, ErrorCode.NOT_ACCEPTABLE)
                            .containsEntry(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE)
                            .containsEntry(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE)
                            .hasSize(5);
                    assertThat(errorProperties.clientErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(errorProperties.serverErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                    assertThat(errorProperties.problems()).hasSize(7);
                    assertThat(context).hasSingleBean(ProblemDetailFactory.class);
                });
    }

    @Test
    void completeConfigurationStarts() {
        runner.withPropertyValues(COMPLETE_ERROR_PROPERTIES.toArray(String[]::new))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void statusCodeWithoutProblemDefinitionFailsStartup() {
        runner.withPropertyValues(completePropertiesWithout("app.errors.problems[ROUTE_NOT_FOUND]"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause().hasMessageContaining("ROUTE_NOT_FOUND");
                });
    }

    @Test
    void clientErrorCodeWithoutProblemDefinitionFailsStartup() {
        runner.withPropertyValues(completePropertiesWithout("app.errors.problems[VALIDATION_ERROR]"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause().hasMessageContaining("VALIDATION_ERROR");
                });
    }

    @Test
    void serverErrorCodeWithoutProblemDefinitionFailsStartup() {
        runner.withPropertyValues(completePropertiesWithout("app.errors.server-error-code",
                        "app.errors.server-error-code=SERVICE_UNAVAILABLE"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause().hasMessageContaining("SERVICE_UNAVAILABLE");
                });
    }

    @Test
    void blankProblemDetailFailsStartup() {
        runner.withPropertyValues(completePropertiesWithout("app.errors.problems[INTERNAL_ERROR].detail",
                        "app.errors.problems[INTERNAL_ERROR].detail= "))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void missingServerErrorCodeFailsStartup() {
        runner.withPropertyValues(completePropertiesWithout("app.errors.server-error-code"))
                .run(context -> assertThat(context).hasFailed());
    }
}
