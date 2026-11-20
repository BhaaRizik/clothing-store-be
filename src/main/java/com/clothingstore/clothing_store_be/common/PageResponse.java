package com.clothingstore.clothing_store_be.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Stable pagination envelope for list endpoints (never serialize Spring's {@code Page} directly). */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> source, Function<? super S, ? extends T> mapper) {
        List<T> items = source.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(items, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
