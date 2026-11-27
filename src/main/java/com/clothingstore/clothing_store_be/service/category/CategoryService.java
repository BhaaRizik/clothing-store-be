package com.clothingstore.clothing_store_be.service.category;

import com.clothingstore.clothing_store_be.dto.category.CategoryResponse;
import com.clothingstore.clothing_store_be.exception.category.CategoryNotFoundException;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapper;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
import com.clothingstore.clothing_store_be.validation.CatalogSlugValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CatalogSlugValidator catalogSlugValidator;

    public List<CategoryResponse> listActiveCategories() {
        return categoryMapper.toCategoryResponses(categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc());
    }

    public CategoryResponse getActiveCategory(String slug) {
        return categoryMapper.toCategoryResponse(requireActiveCategory(slug));
    }

    public Category requireActiveCategory(String slug) {
        return Optional.ofNullable(slug)
                .filter(catalogSlugValidator::isValidCatalogSlug)
                .flatMap(categoryRepository::findBySlugAndActiveTrue)
                .orElseThrow(CategoryNotFoundException::new);
    }

    public Optional<Category> findCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId);
    }
}
