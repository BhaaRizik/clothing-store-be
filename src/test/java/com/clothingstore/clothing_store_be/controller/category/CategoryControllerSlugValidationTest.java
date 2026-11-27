package com.clothingstore.clothing_store_be.controller.category;

import com.clothingstore.clothing_store_be.dto.category.CategoryResponse;
import com.clothingstore.clothing_store_be.mapper.category.CategoryMapper;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
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
@WebMvcTest(CategoryController.class)
@Import(CategoryService.class)
class CategoryControllerSlugValidationTest {

    private static final String SLUG_MESSAGE = "must be a lowercase kebab-case slug of at most 200 characters";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private CategoryMapper categoryMapper;

    private static void expectSlugValidationError(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("slug"))
                .andExpect(jsonPath("$.errors[0].message").value(SLUG_MESSAGE));
    }

    @ParameterizedTest(name = "slug={0}")
    @ValueSource(strings = {"MEN", "Men", "men_shirts", "men shirts", "-men", "men-", "men--shirts", "men.shirts",
            "{$ne: null}"})
    void malformedSlugReturns400OnSlugFieldWithoutQueryingRepository(String slug) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/categories/{slug}", slug));

        expectSlugValidationError(result);
        result.andExpect(content().string(not(containsString(slug))));
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void slugLongerThanConfiguredMaximumReturns400WithoutQueryingRepository() throws Exception {
        expectSlugValidationError(mockMvc.perform(get("/api/v1/categories/{slug}", "a".repeat(201))));
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void slugAtConfiguredMaximumIsLookedUpAndFound() throws Exception {
        String slug = "a".repeat(200);
        Category category = new Category("c1", slug, "Long", null, null, null, 1, true, Instant.EPOCH, Instant.EPOCH);
        when(categoryRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.of(category));
        when(categoryMapper.toCategoryResponse(category)).thenReturn(new CategoryResponse("c1", slug, "Long", null, null, 1));

        mockMvc.perform(get("/api/v1/categories/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));
        verify(categoryRepository).findBySlugAndActiveTrue(slug);
    }

    @Test
    void wellFormedUnknownSlugReturns404CategoryNotFound() throws Exception {
        when(categoryRepository.findBySlugAndActiveTrue("archive")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/categories/{slug}", "archive"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Category not found."));
        verify(categoryRepository).findBySlugAndActiveTrue("archive");
    }
}
