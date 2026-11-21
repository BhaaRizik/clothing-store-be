package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.RoundingMode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoneyConstants {

    public static final int AMOUNT_FRACTION_DIGITS = 2;
    public static final RoundingMode AMOUNT_ROUNDING_MODE = RoundingMode.HALF_UP;
}
