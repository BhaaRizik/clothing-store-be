package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.CategoryResponse;
import com.clothingstore.clothing_store_be.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Public storefront categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List active categories", description = "Sorted by displayOrder ascending, then name.")
    @ApiResponse(responseCode = "200", description = "Active categories")
    public List<CategoryResponse> listCategories() {
        return categoryService.listActiveCategories();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get an active category by slug")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND: missing or inactive",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CategoryResponse getCategory(@PathVariable String slug) {
        return categoryService.getActiveCategory(slug);
    }
}
