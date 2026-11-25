package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductImageResponse;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    ImageResponse toImageResponse(ProductImage productImage);

    ProductImageResponse toProductImageResponse(ProductImage productImage);

    default List<ProductImageResponse> toProductImageResponses(List<ProductImage> productImages) {
        return Optional.ofNullable(productImages)
                .orElseGet(List::of)
                .stream()
                .map(this::toProductImageResponse)
                .toList();
    }

    @Named("toPrimaryImageResponse")
    default ImageResponse toPrimaryImageResponse(List<ProductImage> productImages) {
        return Optional.ofNullable(productImages)
                .flatMap(images -> images.stream().findFirst())
                .map(this::toImageResponse)
                .orElse(null);
    }
}
