package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.dto.product.ProductColorResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductVariantResponse;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    @Mapping(target = "available", source = "stockQuantity", qualifiedByName = "isInStock")
    ProductVariantResponse toProductVariantResponse(ProductVariant productVariant);

    ProductColorResponse toProductColorResponse(ProductColor productColor);

    default List<ProductVariantResponse> toProductVariantResponses(List<ProductVariant> productVariants) {
        return Optional.ofNullable(productVariants)
                .orElseGet(List::of)
                .stream()
                .map(this::toProductVariantResponse)
                .toList();
    }

    @Named("isInStock")
    default boolean isInStock(Integer stockQuantity) {
        return Optional.ofNullable(stockQuantity).filter(quantity -> quantity > 0).isPresent();
    }
}
