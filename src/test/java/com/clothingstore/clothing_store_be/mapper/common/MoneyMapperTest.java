package com.clothingstore.clothing_store_be.mapper.common;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.common.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyMapperTest {

    private final MoneyMapper moneyMapper = new MoneyMapperImpl();
    private final StoreProperties storeProperties = new StoreProperties("ILS");

    @Test
    void formatsWithExactlyTwoDecimalsAndConfiguredCurrency() {
        assertThat(moneyMapper.toMoney(new BigDecimal("49.9"), storeProperties)).isEqualTo(new Money("49.90", "ILS"));
        assertThat(moneyMapper.toMoney(new BigDecimal("189"), storeProperties)).isEqualTo(new Money("189.00", "ILS"));
        assertThat(moneyMapper.toMoney(new BigDecimal("1E+3"), storeProperties)).isEqualTo(new Money("1000.00", "ILS"));
        assertThat(moneyMapper.toMoney(new BigDecimal("19.995"), storeProperties)).isEqualTo(new Money("20.00", "ILS"));
    }

    @Test
    void roundsHalfUpRatherThanHalfEven() {
        assertThat(moneyMapper.toMoney(new BigDecimal("0.125"), storeProperties)).isEqualTo(new Money("0.13", "ILS"));
        assertThat(moneyMapper.toMoney(new BigDecimal("0.124"), storeProperties)).isEqualTo(new Money("0.12", "ILS"));
    }

    @Test
    void usesTheConfiguredCurrency() {
        assertThat(moneyMapper.toMoney(BigDecimal.TEN, new StoreProperties("EUR"))).isEqualTo(new Money("10.00", "EUR"));
    }

    @Test
    void nullAmountGivesNull() {
        assertThat(moneyMapper.toMoney(null, storeProperties)).isNull();
    }
}
