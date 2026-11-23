package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.model.product.ProductSort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSortMapperTest {

    private final ProductSortMapper productSortMapper = new ProductSortMapperImpl();

    @ParameterizedTest
    @CsvSource({"newest, NEWEST", "name, NAME", "price_asc, PRICE_ASC", "price_desc, PRICE_DESC"})
    void mapsSortParameterToSortOption(String sortParameter, ProductSort expectedSort) {
        assertThat(productSortMapper.toProductSort(sortParameter)).isEqualTo(expectedSort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRICE_DESC", "cheapest", ""})
    void unknownSortParameterIsRejected(String sortParameter) {
        assertThatThrownBy(() -> productSortMapper.toProductSort(sortParameter))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
