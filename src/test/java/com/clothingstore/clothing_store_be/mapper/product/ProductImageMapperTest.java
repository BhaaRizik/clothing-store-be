package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageMapperTest {

    private final ProductImageMapper productImageMapper = new ProductImageMapperImpl();

    private final List<ProductImage> images = List.of(new ProductImage("u1", "Front"), new ProductImage("u2", "Back"));

    @Test
    void mapsAllImagesInOrder() {
        assertThat(productImageMapper.toImageResponses(images))
                .containsExactly(new ImageResponse("u1", "Front"), new ImageResponse("u2", "Back"));
    }

    @Test
    void nullImagesMapToEmptyList() {
        assertThat(productImageMapper.toImageResponses(null)).isEmpty();
    }

    @Test
    void primaryImageIsTheFirstImage() {
        assertThat(productImageMapper.toPrimaryImageResponse(images)).isEqualTo(new ImageResponse("u1", "Front"));
    }

    @Test
    void primaryImageIsNullWhenImagesAreNullOrEmpty() {
        assertThat(productImageMapper.toPrimaryImageResponse(null)).isNull();
        assertThat(productImageMapper.toPrimaryImageResponse(List.of())).isNull();
    }
}
