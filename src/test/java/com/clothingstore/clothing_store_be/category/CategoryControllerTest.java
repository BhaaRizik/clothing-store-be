package com.clothingstore.clothing_store_be.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void listsCategories() throws Exception {
        when(categoryService.listActiveCategories()).thenReturn(List.of(
                new CategoryResponse("c1", "men", "Men", "Desc", "http://img", 1),
                new CategoryResponse("c2", "kids", "Kids", null, null, 3)));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("c1"))
                .andExpect(jsonPath("$[0].slug").value("men"))
                .andExpect(jsonPath("$[0].name").value("Men"))
                .andExpect(jsonPath("$[0].description").value("Desc"))
                .andExpect(jsonPath("$[0].imageUrl").value("http://img"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                // nullable fields are present as explicit nulls
                .andExpect(jsonPath("$[1].description").isEmpty())
                .andExpect(jsonPath("$[1].imageUrl").isEmpty());
    }

    @Test
    void getsCategoryBySlug() throws Exception {
        when(categoryService.getActiveCategory("men")).thenReturn(new CategoryResponse("c1", "men", "Men", null, null, 1));

        mockMvc.perform(get("/api/v1/categories/men"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("men"));
    }

    @Test
    void unknownCategoryReturns404ProblemDetail() throws Exception {
        when(categoryService.getActiveCategory("archive")).thenThrow(new CategoryNotFoundException());

        mockMvc.perform(get("/api/v1/categories/archive"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void unexpectedErrorReturnsGeneric500WithoutInternals() throws Exception {
        when(categoryService.listActiveCategories()).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void corsAllowsConfiguredOrigin() throws Exception {
        when(categoryService.listActiveCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsPreflightAllowsGetFromConfiguredOrigin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/v1/categories")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void corsPreflightRejectsWriteMethod() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/v1/categories")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
