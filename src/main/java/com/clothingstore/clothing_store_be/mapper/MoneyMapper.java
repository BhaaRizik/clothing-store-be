package com.clothingstore.clothing_store_be.mapper;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.constants.MoneyConstants;
import com.clothingstore.clothing_store_be.dto.Money;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface MoneyMapper {

    default Money toMoney(BigDecimal amount, @Context StoreProperties storeProperties) {
        return Optional.ofNullable(amount)
                .map(value -> value.setScale(MoneyConstants.AMOUNT_FRACTION_DIGITS, MoneyConstants.AMOUNT_ROUNDING_MODE))
                .map(scaledAmount -> new Money(scaledAmount.toPlainString(), storeProperties.currency()))
                .orElse(null);
    }
}
