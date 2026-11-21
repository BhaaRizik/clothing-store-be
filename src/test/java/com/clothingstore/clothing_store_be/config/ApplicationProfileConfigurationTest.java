package com.clothingstore.clothing_store_be.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationProfileConfigurationTest {

    @Configuration(proxyBeanMethods = false)
    static class EnvironmentOnlyConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({CorsProperties.class, StoreProperties.class, CatalogPaginationProperties.class})
    static class ApplicationPropertiesConfiguration {
    }

    private static ConfigurableApplicationContext start(Class<?> configuration, Map<String, Object> osEnvironment,
            String... arguments) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, osEnvironment));
        return new SpringApplicationBuilder(configuration)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(arguments);
    }

    @Test
    void localProfileResolvesPlaceholdersToLocalValues() {
        try (ConfigurableApplicationContext context = start(ApplicationPropertiesConfiguration.class, Map.of(),
                "--spring.profiles.active=local")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.mongodb.uri")).isEqualTo("mongodb://localhost:27018/clothing_store");
            assertThat(context.getBean(CorsProperties.class).allowedOrigins()).containsExactly("http://localhost:5173");
            assertThat(context.getBean(StoreProperties.class).currency()).isEqualTo("ILS");
        }
    }

    @Test
    void paginationLimitsAreBoundFromApplicationYaml() {
        try (ConfigurableApplicationContext context = start(ApplicationPropertiesConfiguration.class, Map.of(),
                "--spring.profiles.active=local")) {
            assertThat(context.getBean(CatalogPaginationProperties.class))
                    .isEqualTo(new CatalogPaginationProperties(10000, 50, 20));
        }
    }

    @Test
    void noActiveProfileLeavesEnvironmentSpecificPlaceholdersUnresolved() {
        try (ConfigurableApplicationContext context = start(EnvironmentOnlyConfiguration.class, Map.of())) {
            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThatThrownBy(() -> context.getEnvironment().getRequiredProperty("spring.mongodb.uri"))
                    .hasMessageContaining("Could not resolve placeholder 'MONGODB_URI'");
        }
    }

    @Test
    void prodProfileLeavesEnvironmentSpecificValuesEmpty() {
        try (ConfigurableApplicationContext context = start(EnvironmentOnlyConfiguration.class, Map.of(),
                "--spring.profiles.active=prod")) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.mongodb.uri")).isEmpty();
            assertThat(environment.getProperty("app.cors.allowed-origins")).isEmpty();
            assertThat(environment.getProperty("store.currency")).isEqualTo("ILS");
        }
    }

    @Test
    void prodProfileWithoutEnvironmentVariablesFailsFast() {
        assertThatThrownBy(() -> start(ApplicationPropertiesConfiguration.class, Map.of(), "--spring.profiles.active=prod"))
                .hasRootCauseInstanceOf(BindValidationException.class);
    }

    @Test
    void operatingSystemEnvironmentVariablesOverrideProfileValues() {
        Map<String, Object> osEnvironment = Map.of(
                "MONGODB_URI", "mongodb://deployment-host:27017/clothing_store",
                "FRONTEND_ORIGINS", "https://shop.example.com,https://www.shop.example.com",
                "STORE_CURRENCY", "EUR");
        try (ConfigurableApplicationContext context = start(ApplicationPropertiesConfiguration.class, osEnvironment,
                "--spring.profiles.active=prod")) {
            assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                    .isEqualTo("mongodb://deployment-host:27017/clothing_store");
            assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                    .containsExactly("https://shop.example.com", "https://www.shop.example.com");
            assertThat(context.getBean(StoreProperties.class).currency()).isEqualTo("EUR");
        }
    }

    @Test
    void environmentVariableOverridesLocalProfileValue() {
        try (ConfigurableApplicationContext context = start(EnvironmentOnlyConfiguration.class,
                Map.of("MONGODB_URI", "mongodb://override-host:27017/clothing_store"), "--spring.profiles.active=local")) {
            assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                    .isEqualTo("mongodb://override-host:27017/clothing_store");
        }
    }

    @Test
    void noActiveProfileLeavesPlaceholdersUnresolvedAndFailsValidation() {
        assertThatThrownBy(() -> start(ApplicationPropertiesConfiguration.class, Map.of()))
                .hasRootCauseInstanceOf(BindValidationException.class);
    }

    @Test
    void emptyMongoUriFromProdProfileFailsStartupInsteadOfFallingBackToLocalhost() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
                .withPropertyValues("spring.mongodb.uri=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .rootCause().hasMessageContaining("The connection string is invalid");
                });
    }

    @Test
    void validMongoUriCreatesClientWithoutConnecting() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
                .withPropertyValues("spring.mongodb.uri=mongodb://localhost:27018/clothing_store")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(MongoClient.class));
    }
}
