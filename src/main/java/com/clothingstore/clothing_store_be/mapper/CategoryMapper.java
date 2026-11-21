package com.clothingstore.clothing_store_be.mapper;

import com.clothingstore.clothing_store_be.dto.CategoryReferenceResponse;
import com.clothingstore.clothing_store_be.dto.CategoryResponse;
import com.clothingstore.clothing_store_be.model.Category;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponses(List<Category> categories);

    CategoryReferenceResponse toCategoryReferenceResponse(Category category);
}
