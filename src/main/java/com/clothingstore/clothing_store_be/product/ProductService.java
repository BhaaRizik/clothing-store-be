package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.category.Category;
import com.clothingstore.clothing_store_be.category.CategoryService;
import com.clothingstore.clothing_store_be.common.MoneyFormatter;
import com.clothingstore.clothing_store_be.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Read-only storefront use cases for products. Only active products are visible. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final MoneyFormatter moneyFormatter;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
            MoneyFormatter moneyFormatter) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.moneyFormatter = moneyFormatter;
    }

    /**
     * @param categorySlug optional active category slug
     * @param featured     optional featured filter
     * @throws com.clothingstore.clothing_store_be.category.CategoryNotFoundException for an unknown/inactive slug
     */
    public PageResponse<ProductCardResponse> listProducts(String categorySlug, Boolean featured, int page, int size,
            ProductSort sort) {
        String categoryId = categorySlug == null ? null : categoryService.requireActiveCategory(categorySlug).id();
        PageRequest pageRequest = PageRequest.of(page, size, sort.sort());
        return PageResponse.from(productRepository.findActive(categoryId, featured, pageRequest), this::toCard);
    }

    public ProductDetailsResponse getProduct(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug).orElseThrow(ProductNotFoundException::new);
        Category category = categoryService.findById(product.categoryId())
                .orElseThrow(() -> new IllegalStateException(
                        "Data integrity error: product " + product.id() + " references a missing category"));
        return new ProductDetailsResponse(
                product.id(),
                product.slug(),
                product.name(),
                product.description(),
                product.brand(),
                product.images().stream().map(ImageResponse::from).toList(),
                moneyFormatter.format(product.basePrice()),
                moneyFormatter.format(product.salePrice()),
                new ProductDetailsResponse.CategoryRef(category.slug(), category.name()));
    }

    ProductCardResponse toCard(Product product) {
        ImageResponse primaryImage = product.images().isEmpty() ? null : ImageResponse.from(product.images().getFirst());
        return new ProductCardResponse(
                product.id(),
                product.slug(),
                product.name(),
                primaryImage,
                moneyFormatter.format(product.basePrice()),
                moneyFormatter.format(product.salePrice()));
    }
}
