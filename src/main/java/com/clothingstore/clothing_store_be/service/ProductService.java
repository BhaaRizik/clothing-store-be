package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.dto.PageResponse;
import com.clothingstore.clothing_store_be.dto.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.ProductDetailsResponse;
import com.clothingstore.clothing_store_be.exception.ProductNotFoundException;
import com.clothingstore.clothing_store_be.mapper.ProductMapper;
import com.clothingstore.clothing_store_be.model.Category;
import com.clothingstore.clothing_store_be.model.Product;
import com.clothingstore.clothing_store_be.model.ProductSort;
import com.clothingstore.clothing_store_be.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final StoreProperties storeProperties;

    public PageResponse<ProductCardResponse> listProducts(String categorySlug, Boolean featured, int page, int size,
            ProductSort sort) {
        Optional<String> categoryId = Optional.ofNullable(categorySlug)
                .map(slug -> categoryService.requireActiveCategory(slug).id());
        PageRequest pageRequest = PageRequest.of(page, size, sort.getSort());
        return productMapper.toProductCardPageResponse(
                productRepository.findActiveProductsByFilters(categoryId, Optional.ofNullable(featured), pageRequest),
                storeProperties);
    }

    public ProductDetailsResponse getProduct(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug).orElseThrow(ProductNotFoundException::new);
        Category category = categoryService.findCategoryById(product.categoryId())
                .orElseThrow(() -> new IllegalStateException(
                        ErrorMessages.MISSING_PRODUCT_CATEGORY_FORMAT.formatted(product.id())));
        return productMapper.toProductDetailsResponse(product, category, storeProperties);
    }
}
