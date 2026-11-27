package com.clothingstore.clothing_store_be.controller.product;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapper;
import com.clothingstore.clothing_store_be.mapper.product.ProductMapper;
import com.clothingstore.clothing_store_be.mapper.product.ProductSortMapperImpl;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
import com.clothingstore.clothing_store_be.service.product.ProductService;
import com.clothingstore.clothing_store_be.validation.CatalogSlugValidator;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(ProductController.class)
@Import({ProductService.class, CategoryService.class, CatalogSlugValidator.class, ProductSortMapperImpl.class})
@EnableConfigurationProperties({CatalogSlugProperties.class, StoreProperties.class})
class ProductControllerSlugValidationTest {

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

    private static void expectNotFound(ResultActions result, String code, String detail) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private void verifyNoDatabaseAccess() {
        verifyNoInteractions(productRepository, productVariantRepository, categoryRepository, productMapper,
                categoryMapper);
    }

    @ParameterizedTest(name = "slug={0}")
    @ValueSource(strings = {"Oxford-Shirt", "OXFORD", "oxford_shirt", "oxford shirt", "-oxford", "oxford-",
            "oxford--shirt", "oxford.shirt", "{$ne: null}"})
    void malformedProductSlugReturnsProductNotFoundWithoutQueryingRepositories(String slug) throws Exception {
        expectNotFound(mockMvc.perform(get("/api/v1/products/{slug}", slug)), "PRODUCT_NOT_FOUND", "Product not found.");
        verifyNoDatabaseAccess();
    }

    @Test
    void productSlugLongerThanConfiguredMaximumReturnsProductNotFoundWithoutQueryingRepositories() throws Exception {
        expectNotFound(mockMvc.perform(get("/api/v1/products/{slug}", "a".repeat(201))), "PRODUCT_NOT_FOUND",
                "Product not found.");
        verifyNoDatabaseAccess();
    }

    @Test
    void wellFormedUnknownProductSlugAtConfiguredMaximumQueriesRepositoryAndReturnsSameProblem() throws Exception {
        String slug = "a".repeat(200);
        when(productRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.empty());

        expectNotFound(mockMvc.perform(get("/api/v1/products/{slug}", slug)), "PRODUCT_NOT_FOUND", "Product not found.");
        verify(productRepository).findBySlugAndActiveTrue(slug);
    }

    @ParameterizedTest(name = "category={0}")
    @ValueSource(strings = {"MEN", "Men", "men_shirts", "men shirts", "-men", "men-", "men--shirts", "{$ne: null}"})
    void malformedCategoryFilterReturnsCategoryNotFoundWithoutQueryingRepositories(String category) throws Exception {
        expectNotFound(mockMvc.perform(get("/api/v1/products").param("category", category)), "CATEGORY_NOT_FOUND",
                "Category not found.");
        verifyNoDatabaseAccess();
    }

    @Test
    void categoryFilterLongerThanConfiguredMaximumReturnsCategoryNotFoundWithoutQueryingRepositories()
            throws Exception {
        expectNotFound(mockMvc.perform(get("/api/v1/products").param("category", "a".repeat(201))),
                "CATEGORY_NOT_FOUND", "Category not found.");
        verifyNoDatabaseAccess();
    }

    @Test
    void wellFormedUnknownCategoryFilterQueriesRepositoryAndReturnsSameProblem() throws Exception {
        when(categoryRepository.findBySlugAndActiveTrue("archive")).thenReturn(Optional.empty());

        expectNotFound(mockMvc.perform(get("/api/v1/products").param("category", "archive")), "CATEGORY_NOT_FOUND",
                "Category not found.");
        verify(categoryRepository).findBySlugAndActiveTrue("archive");
        verifyNoInteractions(productRepository);
    }
}
