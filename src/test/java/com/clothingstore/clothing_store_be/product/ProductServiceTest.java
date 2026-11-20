package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.category.Category;
import com.clothingstore.clothing_store_be.category.CategoryNotFoundException;
import com.clothingstore.clothing_store_be.category.CategoryService;
import com.clothingstore.clothing_store_be.common.Money;
import com.clothingstore.clothing_store_be.common.MoneyFormatter;
import com.clothingstore.clothing_store_be.common.PageResponse;
import com.clothingstore.clothing_store_be.config.StoreProperties;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryService, new MoneyFormatter(new StoreProperties("ILS")));
    }

    static Product product(String slug, List<ProductImage> images, String basePrice, String salePrice) {
        return new Product("id-" + slug, slug, "Name " + slug, "Desc", "cat-1", null, images,
                new BigDecimal(basePrice), salePrice == null ? null : new BigDecimal(salePrice),
                true, false, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void listMapsCardsWithFirstImageAndFormattedPrices() {
        Product withImages = product("shirt", List.of(new ProductImage("u1", "a1"), new ProductImage("u2", "a2")), "49.9", "39.90");
        Product withoutImages = product("tee", List.of(), "19.90", null);
        when(productRepository.findActive(isNull(), isNull(), any()))
                .thenAnswer(inv -> new PageImpl<>(List.of(withImages, withoutImages), inv.getArgument(2), 2));

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
    void nullImagesAreTreatedAsEmpty() {
        Product product = product("x", null, "10.00", null);
        assertThat(product.images()).isEmpty();
        assertThat(productService.toCard(product).primaryImage()).isNull();
    }

    @Test
    void categoryAndFeaturedFiltersArePassedToRepository() {
        Category men = new Category("cat-men", "men", "Men", null, null, null, 1, true, Instant.EPOCH, Instant.EPOCH);
        when(categoryService.requireActiveCategory("men")).thenReturn(men);
        when(productRepository.findActive(eq("cat-men"), eq(true), any()))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(2), 0));

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
        when(productRepository.findActive(isNull(), isNull(), any()))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(2), 0));

        productService.listProducts(null, null, 0, 20, sort);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findActive(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 20,
                Sort.by(new Sort.Order(direction, property), Sort.Order.asc("id"))));
    }

    @Test
    void sortParamParsing() {
        assertThat(ProductSort.fromParam("price_desc")).isEqualTo(ProductSort.PRICE_DESC);
        assertThatThrownBy(() -> ProductSort.fromParam("PRICE_DESC")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getProductReturnsDetailsWithCategoryRef() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1")), "49.90", null);
        when(productRepository.findBySlugAndActiveTrue("shirt")).thenReturn(Optional.of(shirt));
        when(categoryService.findById("cat-1")).thenReturn(Optional.of(
                new Category("cat-1", "men", "Men", null, null, null, 1, true, Instant.EPOCH, Instant.EPOCH)));

        ProductDetailsResponse details = productService.getProduct("shirt");

        assertThat(details.images()).containsExactly(new ImageResponse("u1", "a1"));
        assertThat(details.price()).isEqualTo(new Money("49.90", "ILS"));
        assertThat(details.salePrice()).isNull();
        assertThat(details.brand()).isNull();
        assertThat(details.category()).isEqualTo(new ProductDetailsResponse.CategoryRef("men", "Men"));
    }

    @Test
    void missingOrInactiveProductIsNotFound() {
        when(productRepository.findBySlugAndActiveTrue("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct("gone")).isInstanceOf(ProductNotFoundException.class);
    }
}
