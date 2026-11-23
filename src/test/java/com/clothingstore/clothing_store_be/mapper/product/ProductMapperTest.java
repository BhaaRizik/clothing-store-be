package com.clothingstore.clothing_store_be.mapper.product;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.category.CategoryReferenceResponse;
import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.dto.common.PageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductDetailsResponse;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapperImpl;
import com.clothingstore.clothing_store_be.mapper.common.MoneyMapperImpl;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
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
            new CategoryMapperImpl());
    private final StoreProperties storeProperties = new StoreProperties("ILS");
    private final Category men = new Category("cat-1", "men", "Men", "Desc", null, null, 1, true, Instant.EPOCH, Instant.EPOCH);

    private static Product product(String slug, List<ProductImage> images, String salePrice) {
        return new Product("id-" + slug, slug, "Name " + slug, "Description " + slug, "cat-1", "Acme", images,
                new BigDecimal("49.9"), salePrice == null ? null : new BigDecimal(salePrice), true, true,
                Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void mapsProductCardWithFirstImageAndFormattedPrices() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1"), new ProductImage("u2", "a2")), "39.9");

        assertThat(productMapper.toProductCardResponse(shirt, storeProperties)).isEqualTo(new ProductCardResponse(
                "id-shirt", "shirt", "Name shirt", new ImageResponse("u1", "a1"),
                new Money("49.90", "ILS"), new Money("39.90", "ILS")));
    }

    @Test
    void productCardWithNullImagesHasNullPrimaryImageAndNullSalePrice() {
        ProductCardResponse card = productMapper.toProductCardResponse(product("coat", null, null), storeProperties);

        assertThat(card.primaryImage()).isNull();
        assertThat(card.salePrice()).isNull();
    }

    @Test
    void mapsProductDetailsWithAllImagesAndCategoryReference() {
        Product shirt = product("shirt", List.of(new ProductImage("u1", "a1"), new ProductImage("u2", "a2")), null);

        assertThat(productMapper.toProductDetailsResponse(shirt, men, storeProperties)).isEqualTo(new ProductDetailsResponse(
                "id-shirt", "shirt", "Name shirt", "Description shirt", "Acme",
                List.of(new ImageResponse("u1", "a1"), new ImageResponse("u2", "a2")),
                new Money("49.90", "ILS"), null, new CategoryReferenceResponse("men", "Men")));
    }

    @Test
    void productDetailsWithNullImagesHasEmptyImageList() {
        ProductDetailsResponse details = productMapper.toProductDetailsResponse(product("coat", null, null), men,
                storeProperties);

        assertThat(details.images()).isNotNull().isEmpty();
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
