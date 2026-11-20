package com.clothingstore.clothing_store_be.common;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts stored {@link BigDecimal} prices into the API {@link Money} representation. */
@Component
public class MoneyFormatter {

    private final String currency;

    public MoneyFormatter(StoreProperties storeProperties) {
        this.currency = storeProperties.currency();
    }

    /**
     * @return the formatted money, or {@code null} when {@code amount} is {@code null}
     */
    public Money format(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        // Catalog prices are stored with 2 decimals; HALF_UP only normalizes unexpected extra precision.
        return new Money(amount.setScale(2, RoundingMode.HALF_UP).toPlainString(), currency);
    }
}
