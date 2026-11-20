package com.clothingstore.clothing_store_be.product;

import org.springframework.data.domain.Sort;

import java.util.Arrays;

/**
 * Whitelisted sort options for the product listing. {@code _id} is appended as a tie-breaker so
 * pagination is stable when sort keys are equal.
 */
public enum ProductSort {
    NEWEST("newest", Sort.by(Sort.Order.desc("createdAt"))),
    NAME("name", Sort.by(Sort.Order.asc("name"))),
    PRICE_ASC("price_asc", Sort.by(Sort.Order.asc("basePrice"))),
    PRICE_DESC("price_desc", Sort.by(Sort.Order.desc("basePrice")));

    /** Regex used to validate the {@code sort} query parameter. */
    public static final String PARAM_PATTERN = "newest|name|price_asc|price_desc";

    private final String param;
    private final Sort sort;

    ProductSort(String param, Sort sort) {
        this.param = param;
        this.sort = sort.and(Sort.by(Sort.Order.asc("id")));
    }

    public Sort sort() {
        return sort;
    }

    public static ProductSort fromParam(String value) {
        return Arrays.stream(values())
                .filter(option -> option.param.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported product sort"));
    }
}
