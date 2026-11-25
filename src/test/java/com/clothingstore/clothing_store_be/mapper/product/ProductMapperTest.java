package com.clothingstore.clothing_store_be.mapper.product;

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
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapperImpl;
import com.clothingstore.clothing_store_be.mapper.common.MoneyMapperImpl;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductMapperTest {

    private final ProductMapper productMapper = new ProductMapperImpl(new MoneyMapperImpl(), new ProductImageMapperImpl(),
            new CategoryMapperImpl(), new ProductVariantMapperImpl());
    private final StoreProperties storeProperties = new StoreProperties("ILS");
    private final Category men = new Category("cat-1", "men", "Men", "Desc", null, null, 1, true, Instant.EPOCH, Instant.EPOCH);

    private static Product product(String slug, List<ProductImage> images, String salePrice) {
        return new Product("id-" + slug, slug, "Name " + slug, "Description " + slug, "cat-1", "Acme", images,
                new BigDecimal("49.9"), salePrice == null ? null : new BigDecimal(salePrice), true, true,
                Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void mapsProductCardWithFirstImageAndFormattedPrices() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1", null), new ProductImage("u2", "a2", null)), "39.9");

        assertThat(productMapper.toProductCardResponse(shirt, storeProperties)).isEqualTo(new ProductCardResponse(
                "id-shirt", "shirt", "Name shirt", new ImageResponse("u1", "a1"),
                new Money("49.90", "ILS"), new Money("39.90", "ILS")));
    }

    @Test
    void productCardPrimaryImageIgnoresColorTag() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1", "White")), null);

        assertThat(productMapper.toProductCardResponse(shirt, storeProperties).primaryImage())
                .isEqualTo(new ImageResponse("u1", "a1"));
    }

    @Test
    void productCardWithNullImagesHasNullPrimaryImageAndNullSalePrice() {
        ProductCardResponse card = productMapper.toProductCardResponse(product("coat", null, null), storeProperties);

        assertThat(card.primaryImage()).isNull();
        assertThat(card.salePrice()).isNull();
    }

    @Test
    void mapsProductDetailsWithAllImagesAndCategoryReference() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1", "Blue"), new ProductImage("u2", "a2", null)), null);

        assertThat(productMapper.toProductDetailsResponse(shirt, men, List.of(), storeProperties))
                .isEqualTo(new ProductDetailsResponse(
                        "id-shirt", "shirt", "Name shirt", "Description shirt", "Acme",
                        List.of(new ProductImageResponse("u1", "a1", "Blue"), new ProductImageResponse("u2", "a2", null)),
                        new Money("49.90", "ILS"), null, new CategoryReferenceResponse("men", "Men"), List.of()));
    }

    @Test
    void productDetailsWithNullImagesHasEmptyImageList() {
        ProductDetailsResponse details = productMapper.toProductDetailsResponse(product("coat", null, null), men,
                List.of(), storeProperties);

        assertThat(details.images()).isNotNull().isEmpty();
    }

    @Test
    void productDetailsIncludeVariantsInGivenOrder() {
        List<ProductVariant> variants = List.of(
                new ProductVariant("v1", "id-shirt", "SKU-S", "S", new ProductColor("Blue", "#1E3A8A"), 3, true, 10,
                        Instant.EPOCH, Instant.EPOCH),
                new ProductVariant("v2", "id-shirt", "SKU-M", "M", null, 0, true, 20, Instant.EPOCH, Instant.EPOCH));

        ProductDetailsResponse details = productMapper.toProductDetailsResponse(product("shirt", List.of(), null), men,
                variants, storeProperties);

        assertThat(details.variants()).containsExactly(
                new ProductVariantResponse("v1", "S", new ProductColorResponse("Blue", "#1E3A8A"), true),
                new ProductVariantResponse("v2", "M", null, false));
    }

    @Test
    void productDetailsWithNullVariantsHaveEmptyVariantList() {
        ProductDetailsResponse details = productMapper.toProductDetailsResponse(product("coat", null, null), men,
                null, storeProperties);

        assertThat(details.variants()).isNotNull().isEmpty();
    }

    @Test
    void mapsPageMetadataAndItems() {
        Page<Product> page = new PageImpl<>(List.of(product("shirt", List.of(), null)), PageRequest.of(1, 1), 3);

        PageResponse<ProductCardResponse> response = productMapper.toProductCardPageResponse(page, storeProperties);

        assertThat(response.items()).extracting(ProductCardResponse::slug).containsExactly("shirt");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalItems()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void emptyPageHasEmptyItemsListNotNull() {
        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        PageResponse<ProductCardResponse> response = productMapper.toProductCardPageResponse(page, storeProperties);

        assertThat(response.items()).isNotNull().isEmpty();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageWithNullContentHasEmptyItemsList() {
        Page<Product> page = mock(Page.class);
        when(page.getContent()).thenReturn(null);
        when(page.getSize()).thenReturn(20);

        PageResponse<ProductCardResponse> response = productMapper.toProductCardPageResponse(page, storeProperties);

        assertThat(response.items()).isNotNull().isEmpty();
        assertThat(response.size()).isEqualTo(20);
    }
}
