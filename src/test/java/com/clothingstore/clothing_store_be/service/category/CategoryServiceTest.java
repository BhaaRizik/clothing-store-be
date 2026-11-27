package com.clothingstore.clothing_store_be.service.category;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import com.clothingstore.clothing_store_be.dto.category.CategoryResponse;
import com.clothingstore.clothing_store_be.exception.category.CategoryNotFoundException;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapperImpl;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
import com.clothingstore.clothing_store_be.validation.CatalogSlugValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void createService() {
        categoryService = new CategoryService(categoryRepository, new CategoryMapperImpl(),
                new CatalogSlugValidator(new CatalogSlugProperties(20)));
    }

    static Category category(String slug, int order) {
        return new Category("id-" + slug, slug, slug.toUpperCase(), null, null, null, order, true,
                Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void listsActiveCategoriesInRepositoryDisplayOrder() {
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category("men", 1), category("women", 2)));

        List<CategoryResponse> result = categoryService.listActiveCategories();

        assertThat(result).extracting(CategoryResponse::slug).containsExactly("men", "women");
        assertThat(result.getFirst()).isEqualTo(new CategoryResponse("id-men", "men", "MEN", null, null, 1));
        verify(categoryRepository).findByActiveTrueOrderByDisplayOrderAscNameAsc();
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"MEN", "Men", "men_shirts", "men shirts", "-men", "men-", "men--shirts", "men.shirts",
            "men/shirts", "men%00", "a23456789012345678901"})
    void malformedSlugIsNotFoundWithoutQueryingRepository(String slug) {
        assertThatThrownBy(() -> categoryService.getActiveCategory(slug)).isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> categoryService.requireActiveCategory(slug))
                .isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void slugAtConfiguredMaximumLengthIsLookedUp() {
        String slug = "a2345678901234567890";
        when(categoryRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.of(category(slug, 1)));

        assertThat(categoryService.getActiveCategory(slug).slug()).isEqualTo(slug);
    }

    @Test
    void findsCategoryByIdRegardlessOfActiveFlag() {
        Category archived = new Category("id-archive", "archive", "Archive", null, null, null, 0, false,
                Instant.EPOCH, Instant.EPOCH);
        when(categoryRepository.findById("id-archive")).thenReturn(Optional.of(archived));

        assertThat(categoryService.findCategoryById("id-archive")).contains(archived);
    }
}
