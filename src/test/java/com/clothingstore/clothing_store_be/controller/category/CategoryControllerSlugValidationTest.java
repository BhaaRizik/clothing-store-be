package com.clothingstore.clothing_store_be.controller.category;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapper;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
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
@WebMvcTest(CategoryController.class)
@Import({CategoryService.class, CatalogSlugValidator.class})
@EnableConfigurationProperties(CatalogSlugProperties.class)
class CategoryControllerSlugValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private CategoryMapper categoryMapper;

    private static void expectCategoryNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Category not found."));
    }

    @ParameterizedTest(name = "slug={0}")
    @ValueSource(strings = {"MEN", "Men", "men_shirts", "men shirts", "-men", "men-", "men--shirts", "men.shirts",
            "{$ne: null}"})
    void malformedSlugReturnsCategoryNotFoundWithoutQueryingRepository(String slug) throws Exception {
        expectCategoryNotFound(mockMvc.perform(get("/api/v1/categories/{slug}", slug)));
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void slugLongerThanConfiguredMaximumReturnsCategoryNotFoundWithoutQueryingRepository() throws Exception {
        expectCategoryNotFound(mockMvc.perform(get("/api/v1/categories/{slug}", "a".repeat(201))));
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void wellFormedUnknownSlugAtConfiguredMaximumQueriesRepositoryAndReturnsSameProblem() throws Exception {
        String slug = "a".repeat(200);
        when(categoryRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.empty());

        expectCategoryNotFound(mockMvc.perform(get("/api/v1/categories/{slug}", slug)));
        verify(categoryRepository).findBySlugAndActiveTrue(slug);
    }
}
