package com.clothingstore.clothing_store_be.mapper;

import com.clothingstore.clothing_store_be.dto.CategoryReferenceResponse;
import com.clothingstore.clothing_store_be.dto.CategoryResponse;
import com.clothingstore.clothing_store_be.model.Category;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper categoryMapper = new CategoryMapperImpl();

    private final Category men = new Category("c1", "men", "Men", "Clothing for men.", "parent-1", "/images/men.svg",
            3, true, Instant.EPOCH, Instant.EPOCH);

    @Test
    void mapsCategoryToPublicResponseWithoutInternalFields() {
        assertThat(categoryMapper.toCategoryResponse(men))
                .isEqualTo(new CategoryResponse("c1", "men", "Men", "Clothing for men.", "/images/men.svg", 3));
    }

    @Test
    void mapsCategoryListInOrder() {
        Category kids = new Category("c2", "kids", "Kids", null, null, null, 4, true, Instant.EPOCH, Instant.EPOCH);

        assertThat(categoryMapper.toCategoryResponses(List.of(men, kids)))
                .extracting(CategoryResponse::slug).containsExactly("men", "kids");
    }

    @Test
    void mapsCategoryReferenceToSlugAndName() {
        assertThat(categoryMapper.toCategoryReferenceResponse(men)).isEqualTo(new CategoryReferenceResponse("men", "Men"));
    }

    @Test
    void nullCategoryMapsToNull() {
        assertThat(categoryMapper.toCategoryResponse(null)).isNull();
        assertThat(categoryMapper.toCategoryReferenceResponse(null)).isNull();
    }
}
