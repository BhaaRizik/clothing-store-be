package com.clothingstore.clothing_store_be.model.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@RequiredArgsConstructor
public enum ProductSort {
    NEWEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))),
    NAME(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))),
    PRICE_ASC(Sort.by(Sort.Order.asc("basePrice"), Sort.Order.asc("id"))),
    PRICE_DESC(Sort.by(Sort.Order.desc("basePrice"), Sort.Order.asc("id")));

    private final Sort sort;
}
