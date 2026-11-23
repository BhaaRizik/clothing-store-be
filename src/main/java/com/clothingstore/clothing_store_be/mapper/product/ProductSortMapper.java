package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.constants.ProductSortParameters;
import com.clothingstore.clothing_store_be.model.product.ProductSort;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = "spring")
public interface ProductSortMapper {

    @ValueMapping(source = ProductSortParameters.NEWEST, target = "NEWEST")
    @ValueMapping(source = ProductSortParameters.NAME, target = "NAME")
    @ValueMapping(source = ProductSortParameters.PRICE_ASC, target = "PRICE_ASC")
    @ValueMapping(source = ProductSortParameters.PRICE_DESC, target = "PRICE_DESC")
    @ValueMapping(source = MappingConstants.ANY_UNMAPPED, target = MappingConstants.THROW_EXCEPTION)
    ProductSort toProductSort(String sortParameter);
}
