package com.clothingstore.clothing_store_be.controller.product;

import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapper;
import com.clothingstore.clothing_store_be.mapper.product.ProductMapper;
import com.clothingstore.clothing_store_be.mapper.product.ProductSortMapperImpl;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
import com.clothingstore.clothing_store_be.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(ProductController.class)
@Import({ProductService.class, CategoryService.class, ProductSortMapperImpl.class})
@EnableConfigurationProperties(StoreProperties.class)
class ProductControllerSlugValidationTest {

    private static final String SLUG_MESSAGE = "must be a lowercase kebab-case slug of at most 200 characters";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductVariantRepository productVariantRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductMapper productMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    private static void expectSlugValidationError(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(SLUG_MESSAGE));
    }

    private void verifyNoDatabaseAccess() {
        verifyNoInteractions(productRepository, productVariantRepository, categoryRepository, productMapper,
                categoryMapper);
    }

    @ParameterizedTest(name = "slug={0}")
    @ValueSource(strings = {"Oxford-Shirt", "OXFORD", "oxford_shirt", "oxford shirt", "-oxford", "oxford-",
            "oxford--shirt", "oxford.shirt", "{$ne: null}"})
    void malformedProductSlugReturns400OnSlugFieldWithoutQueryingRepositories(String slug) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products/{slug}", slug));

        expectSlugValidationError(result, "slug");
        result.andExpect(content().string(not(containsString(slug))));
        verifyNoDatabaseAccess();
    }

    @Test
    void productSlugLongerThanConfiguredMaximumReturns400WithoutQueryingRepositories() throws Exception {
        expectSlugValidationError(mockMvc.perform(get("/api/v1/products/{slug}", "a".repeat(201))), "slug");
        verifyNoDatabaseAccess();
    }

    @Test
    void wellFormedUnknownProductSlugAtConfiguredMaximumQueriesRepositoryAndReturns404() throws Exception {
        String slug = "a".repeat(200);
        when(productRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/{slug}", slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Product not found."));
        verify(productRepository).findBySlugAndActiveTrue(slug);
    }

    @ParameterizedTest(name = "category={0}")
    @ValueSource(strings = {"", "MEN", "Men", "men_shirts", "men shirts", "-men", "men-", "men--shirts",
            "{$ne: null}"})
    void malformedCategoryFilterReturns400OnCategoryFieldWithoutQueryingRepositories(String category)
            throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products").param("category", category));

        expectSlugValidationError(result, "category");
        if (!category.isEmpty()) {
            result.andExpect(content().string(not(containsString(category))));
        }
        verifyNoDatabaseAccess();
    }

    @Test
    void categoryFilterLongerThanConfiguredMaximumReturns400WithoutQueryingRepositories() throws Exception {
        expectSlugValidationError(mockMvc.perform(get("/api/v1/products").param("category", "a".repeat(201))),
                "category");
        verifyNoDatabaseAccess();
    }

    @Test
    void malformedCategoryAndInvalidPaginationAreAllListed() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("category", "Men").param("page", "-1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("category"))
                .andExpect(jsonPath("$.errors[1].field").value("page"))
                .andExpect(jsonPath("$.errors[2].field").value("size"));
        verifyNoDatabaseAccess();
    }

    @Test
    void wellFormedUnknownCategoryFilterQueriesRepositoryAndReturns404() throws Exception {
        when(categoryRepository.findBySlugAndActiveTrue("archive")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products").param("category", "archive"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Category not found."));
        verify(categoryRepository).findBySlugAndActiveTrue("archive");
        verifyNoInteractions(productRepository);
    }
}
