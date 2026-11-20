package com.clothingstore.clothing_store_be.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** Binding/validation of {@code app.cors.allowed-origins} (FRONTEND_ORIGINS): no wildcard may slip through. */
class CorsPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void validCommaSeparatedListBindsWithSpacesTrimmed() {
        runner.withPropertyValues("app.cors.allowed-origins= http://localhost:5173 , https://shop.example.com:8443,https://www.shop-example.co.il")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CorsProperties.class).allowedOrigins()).containsExactly(
                            "http://localhost:5173", "https://shop.example.com:8443", "https://www.shop-example.co.il");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "*",
            "http://localhost:5173,*",
            "localhost:5173",
            "https://shop.example.com/",
            "https://shop.example.com/app",
            "https://*.example.com",
            "ftp://shop.example.com",
            "http://localhost:5173,,https://shop.example.com"})
    void invalidOriginFailsStartup(String value) {
        runner.withPropertyValues("app.cors.allowed-origins=" + value)
                .run(CorsPropertiesTest::assertValidationFailure);
    }

    @Test
    void emptyValueFailsStartup() {
        runner.withPropertyValues("app.cors.allowed-origins=")
                .run(CorsPropertiesTest::assertValidationFailure);
    }

    @Test
    void missingValueFailsStartup() {
        runner.run(CorsPropertiesTest::assertValidationFailure);
    }

    private static void assertValidationFailure(AssertableApplicationContext context) {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
    }
}
