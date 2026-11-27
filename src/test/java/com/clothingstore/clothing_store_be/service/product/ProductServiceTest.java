package com.clothingstore.clothing_store_be.service.product;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.category.CategoryReferenceResponse;
import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.dto.common.PageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductColorResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductDetailsResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductImageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductVariantResponse;
import com.clothingstore.clothing_store_be.exception.category.CategoryNotFoundException;
import com.clothingstore.clothing_store_be.exception.product.ProductNotFoundException;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapperImpl;
import com.clothingstore.clothing_store_be.mapper.common.MoneyMapperImpl;
import com.clothingstore.clothing_store_be.mapper.product.ProductImageMapperImpl;
import com.clothingstore.clothing_store_be.mapper.product.ProductMapperImpl;
import com.clothingstore.clothing_store_be.mapper.product.ProductVariantMapperImpl;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import com.clothingstore.clothing_store_be.model.product.ProductSort;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private CategoryService categoryService;

    private ProductService productService;

    @BeforeEach
    void createService() {
        ProductMapperImpl productMapper = new ProductMapperImpl(new MoneyMapperImpl(), new ProductImageMapperImpl(),
                new CategoryMapperImpl(), new ProductVariantMapperImpl());
        productService = new ProductService(productRepository, productVariantRepository, categoryService, productMapper,
                new StoreProperties("ILS"));
    }

    static Product product(String slug, List<ProductImage> images, String basePrice, String salePrice) {
        return new Product("id-" + slug, slug, "Name " + slug, "Desc", "cat-1", null, images,
                new BigDecimal(basePrice), Optional.ofNullable(salePrice).map(BigDecimal::new).orElse(null),
                true, false, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void listMapsCardsWithFirstImageAndFormattedPrices() {
        Product withImages = product("shirt", List.of(new ProductImage("u1", "a1", null), new ProductImage("u2", "a2", null)), "49.9", "39.90");
        Product withoutImages = product("tee", List.of(), "19.90", null);
        when(productRepository.findActiveProductsByFilters(eq(Optional.empty()), eq(Optional.empty()), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(withImages, withoutImages), invocation.getArgument(2), 2));

        PageResponse<ProductCardResponse> result = productService.listProducts(null, null, 0, 20, ProductSort.NEWEST);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalItems()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.items().get(0)).isEqualTo(new ProductCardResponse("id-shirt", "shirt", "Name shirt",
                new ImageResponse("u1", "a1"), new Money("49.90", "ILS"), new Money("39.90", "ILS")));
        assertThat(result.items().get(1).primaryImage()).isNull();
        assertThat(result.items().get(1).salePrice()).isNull();
        verifyNoInteractions(categoryService);
    }

    @Test
    void categoryAndFeaturedFiltersArePassedToRepository() {
        Category men = new Category("cat-men", "men", "Men", null, null, null, 1, true, Instant.EPOCH, Instant.EPOCH);
        when(categoryService.requireActiveCategory("men")).thenReturn(men);
        when(productRepository.findActiveProductsByFilters(eq(Optional.of("cat-men")), eq(Optional.of(true)), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));

        PageResponse<ProductCardResponse> result = productService.listProducts("men", true, 2, 5, ProductSort.NAME);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
    }

    @Test
    void unknownCategoryIsNotFound() {
        when(categoryService.requireActiveCategory("nope")).thenThrow(new CategoryNotFoundException());

        assertThatThrownBy(() -> productService.listProducts("nope", null, 0, 20, ProductSort.NEWEST))
                .isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(productRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "NEWEST, createdAt, DESC",
            "NAME, name, ASC",
            "PRICE_ASC, basePrice, ASC",
            "PRICE_DESC, basePrice, DESC"})
    void sortOptionsMapToStableMongoSorts(ProductSort sort, String property, Sort.Direction direction) {
        when(productRepository.findActiveProductsByFilters(eq(Optional.empty()), eq(Optional.empty()), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));

        productService.listProducts(null, null, 0, 20, sort);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findActiveProductsByFilters(eq(Optional.empty()), eq(Optional.empty()), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 20,
                Sort.by(new Sort.Order(direction, property), Sort.Order.asc("id"))));
    }

    @Test
    void getProductReturnsDetailsWithCategoryReference() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1", null)), "49.90", null);
        when(productRepository.findBySlugAndActiveTrue("shirt")).thenReturn(Optional.of(shirt));
        when(categoryService.findCategoryById("cat-1")).thenReturn(Optional.of(
                new Category("cat-1", "men", "Men", null, null, null, 1, true, Instant.EPOCH, Instant.EPOCH)));

        ProductDetailsResponse details = productService.getProduct("shirt");

        assertThat(details.images()).containsExactly(new ProductImageResponse("u1", "a1", null));
        assertThat(details.price()).isEqualTo(new Money("49.90", "ILS"));
        assertThat(details.salePrice()).isNull();
        assertThat(details.brand()).isNull();
        assertThat(details.category()).isEqualTo(new CategoryReferenceResponse("men", "Men"));
        assertThat(details.variants()).isNotNull().isEmpty();
    }

    @Test
    void getProductReturnsActiveVariantsFromRepositoryInOrder() {
        Product shirt = product("shirt", List.of(), "49.90", null);
        when(productRepository.findBySlugAndActiveTrue("shirt")).thenReturn(Optional.of(shirt));
        when(categoryService.findCategoryById("cat-1")).thenReturn(Optional.of(
                new Category("cat-1", "men", "Men", null, null, null, 1, true, Instant.EPOCH, Instant.EPOCH)));
        when(productVariantRepository.findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc("id-shirt")).thenReturn(List.of(
                new ProductVariant("v-s", "id-shirt", "SHIRT-S", "S", new ProductColor("Blue", "#1E3A8A"), 0, true, 10,
                        Instant.EPOCH, Instant.EPOCH),
                new ProductVariant("v-m", "id-shirt", "SHIRT-M", "M", null, 7, true, 20, Instant.EPOCH, Instant.EPOCH)));

        ProductDetailsResponse details = productService.getProduct("shirt");

        assertThat(details.variants()).containsExactly(
                new ProductVariantResponse("v-s", "S", new ProductColorResponse("Blue", "#1E3A8A"), false),
                new ProductVariantResponse("v-m", "M", null, true));
        verify(productVariantRepository).findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc("id-shirt");
    }

    @Test
    void productReferencingMissingCategoryIsAnIntegrityError() {
        Product shirt = product("shirt", List.of(), "49.90", null);
        when(productRepository.findBySlugAndActiveTrue("shirt")).thenReturn(Optional.of(shirt));
        when(categoryService.findCategoryById("cat-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct("shirt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Data integrity error: product id-shirt references a missing category");
    }

    @Test
    void missingOrInactiveProductIsNotFound() {
        when(productRepository.findBySlugAndActiveTrue("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct("gone")).isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(productVariantRepository);
    }
}
