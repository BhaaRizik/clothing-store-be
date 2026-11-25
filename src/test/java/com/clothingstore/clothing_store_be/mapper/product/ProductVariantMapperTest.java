package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.dto.product.ProductColorResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductVariantResponse;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVariantMapperTest {

    private final ProductVariantMapper productVariantMapper = new ProductVariantMapperImpl();

    private static ProductVariant variant(String id, String size, ProductColor color, Integer stockQuantity) {
        return new ProductVariant(id, "product-1", "SKU-" + id, size, color, stockQuantity, true, 10,
                Instant.EPOCH, Instant.EPOCH);
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, true", "25, true", "-1, false"})
    void availabilityIsTrueOnlyWhenStockIsPositive(int stockQuantity, boolean expectedAvailable) {
        ProductVariantResponse response = productVariantMapper.toProductVariantResponse(
                variant("v1", "M", new ProductColor("Blue", "#1E3A8A"), stockQuantity));

        assertThat(response.available()).isEqualTo(expectedAvailable);
    }

    @Test
    void missingStockQuantityIsUnavailable() {
        assertThat(productVariantMapper.toProductVariantResponse(variant("v1", "M", null, null)).available()).isFalse();
    }

    @Test
    void variantWithMissingStockActiveAndDisplayOrderStillMaps() {
        ProductVariant bareVariant = new ProductVariant("v1", "product-1", "SKU-v1", null, null, null, null, null,
                null, null);

        assertThat(productVariantMapper.toProductVariantResponse(bareVariant))
                .isEqualTo(new ProductVariantResponse("v1", null, null, false));
    }

    @Test
    void mapsIdSizeAndColor() {
        assertThat(productVariantMapper.toProductVariantResponse(variant("v1", "XL", new ProductColor("Blue", "#1E3A8A"), 4)))
                .isEqualTo(new ProductVariantResponse("v1", "XL", new ProductColorResponse("Blue", "#1E3A8A"), true));
    }

    @Test
    void nullColorMapsToNullColor() {
        assertThat(productVariantMapper.toProductVariantResponse(variant("v1", "M", null, 2)).color()).isNull();
    }

    @Test
    void colorWithoutHexCodeKeepsNameAndNullHexCode() {
        assertThat(productVariantMapper.toProductVariantResponse(variant("v1", "M", new ProductColor("Floral print", null), 2))
                .color()).isEqualTo(new ProductColorResponse("Floral print", null));
    }

    @Test
    void nullSizeMapsToNullSize() {
        assertThat(productVariantMapper.toProductVariantResponse(variant("v1", null, new ProductColor("Black", "#000000"), 2))
                .size()).isNull();
    }

    @Test
    void nullVariantListMapsToEmptyList() {
        assertThat(productVariantMapper.toProductVariantResponses(null)).isNotNull().isEmpty();
    }

    @Test
    void variantListKeepsOrder() {
        List<ProductVariantResponse> responses = productVariantMapper.toProductVariantResponses(List.of(
                variant("v2", "S", null, 0), variant("v1", "M", null, 3)));

        assertThat(responses).extracting(ProductVariantResponse::id).containsExactly("v2", "v1");
    }
}
