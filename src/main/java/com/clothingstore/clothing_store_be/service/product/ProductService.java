package com.clothingstore.clothing_store_be.service.product;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.dto.common.PageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductDetailsResponse;
import com.clothingstore.clothing_store_be.exception.product.ProductNotFoundException;
import com.clothingstore.clothing_store_be.mapper.product.ProductMapper;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductSort;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
import com.clothingstore.clothing_store_be.validation.CatalogSlugValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final StoreProperties storeProperties;
    private final CatalogSlugValidator catalogSlugValidator;

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
        Product product = Optional.ofNullable(slug)
                .filter(catalogSlugValidator::isValidCatalogSlug)
                .flatMap(productRepository::findBySlugAndActiveTrue)
                .orElseThrow(ProductNotFoundException::new);
        Category category = categoryService.findCategoryById(product.categoryId())
                .orElseThrow(() -> new IllegalStateException(
                        ErrorMessages.MISSING_PRODUCT_CATEGORY_FORMAT.formatted(product.id())));
        List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc(product.id());
        return productMapper.toProductDetailsResponse(product, category, variants, storeProperties);
    }
}
