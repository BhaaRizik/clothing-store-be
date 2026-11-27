package com.clothingstore.clothing_store_be.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CartPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(CartPropertiesTestConfiguration.class);

    @Test
    void bindsQuantityAndSimpleDurationFormat() {
        runner.withPropertyValues("app.cart.max-item-quantity=7", "app.cart.inactive-cart-expiry=14d")
                .run(context -> assertThat(context.getBean(CartProperties.class))
                        .isEqualTo(new CartProperties(7, Duration.ofDays(14))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void nonPositiveMaxItemQuantityFailsStartup(String value) {
        runner.withPropertyValues("app.cart.max-item-quantity=" + value, "app.cart.inactive-cart-expiry=30d")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void missingExpiryFailsStartup() {
        runner.withPropertyValues("app.cart.max-item-quantity=10")
                .run(context -> assertThat(context).hasFailed());
    }
}
