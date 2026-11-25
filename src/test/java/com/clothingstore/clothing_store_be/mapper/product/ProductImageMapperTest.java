package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductImageResponse;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageMapperTest {

    private final ProductImageMapper productImageMapper = new ProductImageMapperImpl();

    private final List<ProductImage> images = List.of(new ProductImage("u1", "Front", "White"),
            new ProductImage("u2", "Back", null));

    @Test
    void mapsAllImagesInOrderWithColorTags() {
        assertThat(productImageMapper.toProductImageResponses(images)).containsExactly(
                new ProductImageResponse("u1", "Front", "White"), new ProductImageResponse("u2", "Back", null));
    }

    @Test
    void untaggedImageHasNullColor() {
        assertThat(productImageMapper.toProductImageResponse(new ProductImage("u3", null, null)))
                .isEqualTo(new ProductImageResponse("u3", null, null));
    }

    @Test
    void nullImagesMapToEmptyList() {
        assertThat(productImageMapper.toProductImageResponses(null)).isNotNull().isEmpty();
    }

    @Test
    void primaryImageIsTheFirstImageWithoutColor() {
        assertThat(productImageMapper.toPrimaryImageResponse(images)).isEqualTo(new ImageResponse("u1", "Front"));
    }

    @Test
    void primaryImageIsNullWhenImagesAreNullOrEmpty() {
        assertThat(productImageMapper.toPrimaryImageResponse(null)).isNull();
        assertThat(productImageMapper.toPrimaryImageResponse(List.of())).isNull();
    }
}
