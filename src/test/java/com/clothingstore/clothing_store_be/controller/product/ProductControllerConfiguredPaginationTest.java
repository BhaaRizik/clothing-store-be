package com.clothingstore.clothing_store_be.controller.product;

import com.clothingstore.clothing_store_be.dto.common.PageResponse;
import com.clothingstore.clothing_store_be.mapper.product.ProductSortMapperImpl;
import com.clothingstore.clothing_store_be.model.product.ProductSort;
import com.clothingstore.clothing_store_be.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = ProductController.class, properties = {
        "app.catalog.pagination.max-page=3",
        "app.catalog.pagination.max-page-size=5",
        "app.catalog.pagination.default-page-size=4"})
@Import(ProductSortMapperImpl.class)
class ProductControllerConfiguredPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void defaultPageSizeComesFromConfiguration() throws Exception {
        when(productService.listProducts(null, null, 0, 4, ProductSort.NEWEST))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(4));

        verify(productService).listProducts(null, null, 0, 4, ProductSort.NEWEST);
    }

    @Test
    void configuredMaximumsAreAcceptedInclusive() throws Exception {
        when(productService.listProducts(null, null, 3, 5, ProductSort.NEWEST))
                .thenReturn(new PageResponse<>(List.of(), 3, 5, 0, 0));

        mockMvc.perform(get("/api/v1/products").param("page", "3").param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void valuesAboveConfiguredMaximumsAreRejectedTogether() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("page", "4").param("size", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.errors[0].message").value("must be less than or equal to 3"))
                .andExpect(jsonPath("$.errors[1].field").value("size"))
                .andExpect(jsonPath("$.errors[1].message").value("must be less than or equal to 5"));

        verifyNoInteractions(productService);
    }
}
