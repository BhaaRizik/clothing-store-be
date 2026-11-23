package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromotionConstants {

    public static final String INTERNAL_CTA_PATH_REGEX = "^/(?![/\\\\])[^\\s\\p{Cntrl}]*$";
    public static final Pattern INTERNAL_CTA_PATH_PATTERN = Pattern.compile(INTERNAL_CTA_PATH_REGEX);
}
