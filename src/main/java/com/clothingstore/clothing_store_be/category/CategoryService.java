package com.clothingstore.clothing_store_be.category;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Read-only storefront use cases for categories. Only active categories are visible. */
@Service
public class CategoryService {

    static final Sort DISPLAY_SORT = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name"));

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findByActiveTrue(DISPLAY_SORT).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getActiveCategory(String slug) {
        return CategoryResponse.from(requireActiveCategory(slug));
    }

    /**
     * @throws CategoryNotFoundException if no active category has the given slug
     */
    public Category requireActiveCategory(String slug) {
        return categoryRepository.findBySlugAndActiveTrue(slug).orElseThrow(CategoryNotFoundException::new);
    }

    /** Looks up a category by id regardless of its active flag (used to label products). */
    public Optional<Category> findById(String id) {
        return categoryRepository.findById(id);
    }
}
