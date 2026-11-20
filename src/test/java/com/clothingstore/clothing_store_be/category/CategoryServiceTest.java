package com.clothingstore.clothing_store_be.category;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    static Category category(String slug, int order) {
        return new Category("id-" + slug, slug, slug.toUpperCase(), null, null, null, order, true,
                Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void listsActiveCategoriesSortedByDisplayOrderThenName() {
        when(categoryRepository.findByActiveTrue(CategoryService.DISPLAY_SORT))
                .thenReturn(List.of(category("men", 1), category("women", 2)));

        List<CategoryResponse> result = categoryService.listActiveCategories();

        assertThat(result).extracting(CategoryResponse::slug).containsExactly("men", "women");
        assertThat(result.getFirst()).isEqualTo(new CategoryResponse("id-men", "men", "MEN", null, null, 1));
        verify(categoryRepository).findByActiveTrue(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name")));
    }

    @Test
    void getsActiveCategoryBySlug() {
        when(categoryRepository.findBySlugAndActiveTrue("men")).thenReturn(Optional.of(category("men", 1)));

        assertThat(categoryService.getActiveCategory("men").slug()).isEqualTo("men");
    }

    @Test
    void missingOrInactiveCategoryIsNotFound() {
        when(categoryRepository.findBySlugAndActiveTrue("archive")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getActiveCategory("archive"))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
