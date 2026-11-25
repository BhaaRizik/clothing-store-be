package com.clothingstore.clothing_store_be.controller.product;

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
import com.clothingstore.clothing_store_be.mapper.product.ProductSortMapperImpl;
import com.clothingstore.clothing_store_be.model.product.ProductSort;
import com.clothingstore.clothing_store_be.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(ProductController.class)
@Import(ProductSortMapperImpl.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static final ProductCardResponse CARD = new ProductCardResponse("p1", "shirt", "Shirt",
            new ImageResponse("http://img/1", "Front"), new Money("49.90", "ILS"), null);

    @Test
    void listsProductsWithDefaults() throws Exception {
        when(productService.listProducts(null, null, 0, 20, ProductSort.NEWEST))
                .thenReturn(new PageResponse<>(List.of(CARD), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].id").value("p1"))
                .andExpect(jsonPath("$.items[0].slug").value("shirt"))
                .andExpect(jsonPath("$.items[0].name").value("Shirt"))
                .andExpect(jsonPath("$.items[0].primaryImage.url").value("http://img/1"))
                .andExpect(jsonPath("$.items[0].primaryImage.alt").value("Front"))
                .andExpect(jsonPath("$.items[0].primaryImage.length()").value(2))
                .andExpect(jsonPath("$.items[0].primaryImage.color").doesNotExist())
                .andExpect(jsonPath("$.items[0].price.amount").value("49.90"))
                .andExpect(jsonPath("$.items[0].price.currency").value("ILS"))
                .andExpect(jsonPath("$.items[0].salePrice").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void passesFiltersPagingAndSort() throws Exception {
        when(productService.listProducts("men", true, 1, 50, ProductSort.PRICE_DESC))
                .thenReturn(new PageResponse<>(List.of(), 1, 50, 0, 0));

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "men").param("featured", "true")
                        .param("page", "1").param("size", "50").param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(productService).listProducts("men", true, 1, 50, ProductSort.PRICE_DESC);
    }

    @ParameterizedTest
    @CsvSource({"newest, NEWEST", "name, NAME", "price_asc, PRICE_ASC", "price_desc, PRICE_DESC"})
    void mapsEverySortParameterToItsSortOption(String sortParameter, ProductSort expectedSort) throws Exception {
        when(productService.listProducts(null, null, 0, 20, expectedSort))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/products").param("sort", sortParameter))
                .andExpect(status().isOk());

        verify(productService).listProducts(null, null, 0, 20, expectedSort);
    }

    @ParameterizedTest
    @CsvSource({
            "page, -1",
            "page, 10001",
            "size, 0",
            "size, 51",
            "sort, cheapest",
            "page, abc",
            "featured, maybe"})
    void invalidParametersReturn400ValidationError(String param, String value) throws Exception {
        mockMvc.perform(get("/api/v1/products").param(param, value))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value(param))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());

        verifyNoInteractions(productService);
    }

    @ParameterizedTest
    @CsvSource({"page, 10001, 10000", "size, 51, 50"})
    void valueAboveConfiguredMaximumNamesTheConfiguredLimit(String param, String value, String limit) throws Exception {
        mockMvc.perform(get("/api/v1/products").param(param, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(param))
                .andExpect(jsonPath("$.errors[0].message").value("must be less than or equal to " + limit));

        verifyNoInteractions(productService);
    }

    @Test
    void unknownCategoryReturns404() throws Exception {
        when(productService.listProducts(any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new CategoryNotFoundException());

        mockMvc.perform(get("/api/v1/products").param("category", "archive"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void getsProductDetails() throws Exception {
        when(productService.getProduct("shirt")).thenReturn(new ProductDetailsResponse("p1", "shirt", "Shirt", "Nice",
                null, List.of(new ProductImageResponse("http://img/1", "Front", "Blue"),
                        new ProductImageResponse("http://img/2", null, null)), new Money("49.90", "ILS"),
                new Money("39.90", "ILS"), new CategoryReferenceResponse("men", "Men"), List.of()));

        mockMvc.perform(get("/api/v1/products/shirt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.description").value("Nice"))
                .andExpect(jsonPath("$.brand").isEmpty())
                .andExpect(jsonPath("$.images[0].url").value("http://img/1"))
                .andExpect(jsonPath("$.images[0].color").value("Blue"))
                .andExpect(jsonPath("$.images[1].length()").value(3))
                .andExpect(jsonPath("$.images[1].alt").value(nullValue()))
                .andExpect(jsonPath("$.images[1].color").value(nullValue()))
                .andExpect(jsonPath("$.price.amount").value("49.90"))
                .andExpect(jsonPath("$.salePrice.amount").value("39.90"))
                .andExpect(jsonPath("$.category.slug").value("men"))
                .andExpect(jsonPath("$.category.name").value("Men"))
                .andExpect(jsonPath("$.category.length()").value(2))
                .andExpect(jsonPath("$.variants").isArray())
                .andExpect(jsonPath("$.variants").isEmpty());
    }

    @Test
    void productDetailsExposeVariantsWithoutStockOrSku() throws Exception {
        when(productService.getProduct("shirt")).thenReturn(new ProductDetailsResponse("p1", "shirt", "Shirt", "Nice",
                null, List.of(), new Money("49.90", "ILS"), null, new CategoryReferenceResponse("men", "Men"),
                List.of(new ProductVariantResponse("v1", "M", new ProductColorResponse("Blue", "#1E3A8A"), true),
                        new ProductVariantResponse("v2", null, new ProductColorResponse("Floral print", null), false),
                        new ProductVariantResponse("v3", null, null, true))));

        mockMvc.perform(get("/api/v1/products/shirt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.variants.length()").value(3))
                .andExpect(jsonPath("$.variants[0].length()").value(4))
                .andExpect(jsonPath("$.variants[0].id").value("v1"))
                .andExpect(jsonPath("$.variants[0].size").value("M"))
                .andExpect(jsonPath("$.variants[0].color.name").value("Blue"))
                .andExpect(jsonPath("$.variants[0].color.hexCode").value("#1E3A8A"))
                .andExpect(jsonPath("$.variants[0].color.length()").value(2))
                .andExpect(jsonPath("$.variants[0].available").value(true))
                .andExpect(jsonPath("$.variants[1].size").value(nullValue()))
                .andExpect(jsonPath("$.variants[1].color.hexCode").value(nullValue()))
                .andExpect(jsonPath("$.variants[1].available").value(false))
                .andExpect(jsonPath("$.variants[2].color").value(nullValue()))
                .andExpect(jsonPath("$.variants[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.variants[0].sku").doesNotExist())
                .andExpect(jsonPath("$.variants[0].active").doesNotExist())
                .andExpect(jsonPath("$.variants[0].displayOrder").doesNotExist());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        when(productService.getProduct("gone")).thenThrow(new ProductNotFoundException());

        mockMvc.perform(get("/api/v1/products/gone"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
    }
}
