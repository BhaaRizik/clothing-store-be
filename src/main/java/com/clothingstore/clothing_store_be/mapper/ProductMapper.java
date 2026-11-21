package com.clothingstore.clothing_store_be.mapper;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.PageResponse;
import com.clothingstore.clothing_store_be.dto.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.ProductDetailsResponse;
import com.clothingstore.clothing_store_be.model.Category;
import com.clothingstore.clothing_store_be.model.Product;
import org.mapstruct.Context;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {MoneyMapper.class, ProductImageMapper.class, CategoryMapper.class})
public interface ProductMapper {

    @Mapping(target = "price", source = "basePrice")
    @Mapping(target = "primaryImage", source = "images", qualifiedByName = "toPrimaryImageResponse")
    ProductCardResponse toProductCardResponse(Product product, @Context StoreProperties storeProperties);

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "slug", source = "product.slug")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "description", source = "product.description")
    @Mapping(target = "brand", source = "product.brand")
    @Mapping(target = "images", source = "product.images")
    @Mapping(target = "price", source = "product.basePrice")
    @Mapping(target = "salePrice", source = "product.salePrice")
    @Mapping(target = "category", source = "category")
    ProductDetailsResponse toProductDetailsResponse(Product product, Category category,
            @Context StoreProperties storeProperties);

    @Mapping(target = "items", source = ".", qualifiedByName = "toProductCardItems")
    @Mapping(target = "page", source = "number")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "totalItems", source = "totalElements")
    @Mapping(target = "totalPages", source = "totalPages")
    PageResponse<ProductCardResponse> toProductCardPageResponse(Page<Product> productPage,
            @Context StoreProperties storeProperties);

    @Named("toProductCardItems")
    default List<ProductCardResponse> toProductCardItems(Page<Product> productPage,
            @Context StoreProperties storeProperties) {
        return Optional.ofNullable(productPage.getContent())
                .orElseGet(List::of)
                .stream()
                .map(product -> toProductCardResponse(product, storeProperties))
                .toList();
    }
}
