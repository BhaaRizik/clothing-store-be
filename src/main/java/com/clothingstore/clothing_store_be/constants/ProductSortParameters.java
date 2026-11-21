package com.clothingstore.clothing_store_be.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductSortParameters {

    public static final String NEWEST = "newest";
    public static final String NAME = "name";
    public static final String PRICE_ASC = "price_asc";
    public static final String PRICE_DESC = "price_desc";
}
