package com.clothingstore.clothing_store_be.common;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatterTest {

    private final MoneyFormatter formatter = new MoneyFormatter(new StoreProperties("ILS"));

    @Test
    void formatsWithExactlyTwoDecimalsAndConfiguredCurrency() {
        assertThat(formatter.format(new BigDecimal("49.9"))).isEqualTo(new Money("49.90", "ILS"));
        assertThat(formatter.format(new BigDecimal("189"))).isEqualTo(new Money("189.00", "ILS"));
        assertThat(formatter.format(new BigDecimal("1E+3"))).isEqualTo(new Money("1000.00", "ILS"));
        assertThat(formatter.format(new BigDecimal("19.995"))).isEqualTo(new Money("20.00", "ILS"));
    }

    @Test
    void nullAmountGivesNull() {
        assertThat(formatter.format(null)).isNull();
    }
}
