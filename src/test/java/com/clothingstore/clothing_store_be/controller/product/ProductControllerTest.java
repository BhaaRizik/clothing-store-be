package com.clothingstore.clothing_store_be.controller.product;

import com.clothingstore.clothing_store_be.dto.category.CategoryReferenceResponse;
import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.dto.common.PageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductDetailsResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                null, List.of(new ImageResponse("http://img/1", "Front")), new Money("49.90", "ILS"),
                new Money("39.90", "ILS"), new CategoryReferenceResponse("men", "Men")));

        mockMvc.perform(get("/api/v1/products/shirt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.description").value("Nice"))
                .andExpect(jsonPath("$.brand").isEmpty())
                .andExpect(jsonPath("$.images[0].url").value("http://img/1"))
                .andExpect(jsonPath("$.price.amount").value("49.90"))
                .andExpect(jsonPath("$.salePrice.amount").value("39.90"))
                .andExpect(jsonPath("$.category.slug").value("men"))
                .andExpect(jsonPath("$.category.name").value("Men"))
                .andExpect(jsonPath("$.category.length()").value(2));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        when(productService.getProduct("gone")).thenThrow(new ProductNotFoundException());

        mockMvc.perform(get("/api/v1/products/gone"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
