package com.clothingstore.clothing_store_be.mapper.cart;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.cart.CartItemResponse;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.mapper.common.MoneyMapper;
import com.clothingstore.clothing_store_be.mapper.product.ProductVariantMapper;
import com.clothingstore.clothing_store_be.model.cart.PricedCart;
import com.clothingstore.clothing_store_be.model.cart.PricedCartItem;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.mapstruct.Context;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {MoneyMapper.class, ProductVariantMapper.class})
public interface CartMapper {

    CartResponse toCartResponse(PricedCart pricedCart, @Context StoreProperties storeProperties);

    @Mapping(target = "variantId", source = "item.variantId")
    @Mapping(target = "productId", source = "item.productId")
    @Mapping(target = "productSlug", source = "product.slug")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "image", source = ".", qualifiedByName = "toCartItemImage")
    @Mapping(target = "size", source = "variant.size")
    @Mapping(target = "color", source = "variant.color")
    @Mapping(target = "quantity", source = "item.quantity")
    CartItemResponse toCartItemResponse(PricedCartItem pricedCartItem, @Context StoreProperties storeProperties);

    ImageResponse toCartItemImageResponse(ProductImage productImage);

    default List<CartItemResponse> toCartItemResponses(List<PricedCartItem> pricedCartItems,
            @Context StoreProperties storeProperties) {
        return Optional.ofNullable(pricedCartItems)
                .orElseGet(List::of)
                .stream()
                .map(pricedCartItem -> toCartItemResponse(pricedCartItem, storeProperties))
                .toList();
    }

    @Named("toCartItemImage")
    default ImageResponse toCartItemImage(PricedCartItem pricedCartItem) {
        List<ProductImage> images = Optional.ofNullable(pricedCartItem.product())
                .map(Product::images)
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .toList();
        Optional<String> variantColorName = Optional.ofNullable(pricedCartItem.variant())
                .map(ProductVariant::color)
                .map(ProductColor::name);
        return variantColorName
                .flatMap(colorName -> images.stream().filter(image -> colorName.equals(image.color())).findFirst())
                .or(() -> images.stream().filter(image -> image.color() == null).findFirst())
                .or(() -> images.stream().findFirst())
                .map(this::toCartItemImageResponse)
                .orElse(null);
    }
}
