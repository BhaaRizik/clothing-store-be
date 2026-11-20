package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public storefront products")
public class ProductController {

    static final int MAX_PAGE_SIZE = 50;
    /** Caps the page index so page * size always fits the int skip used by Spring Data MongoDB. */
    static final int MAX_PAGE = 10_000;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List active products as cards", description = "Paginated; optional category and featured filters.")
    @ApiResponse(responseCode = "200", description = "Page of product cards")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: invalid page (0..10000), size (1..50), sort or featured",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND: unknown or inactive category slug",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<ProductCardResponse> listProducts(
            @Parameter(description = "Active category slug") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by featured flag") @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Zero-based page index, 0..10000") @RequestParam(defaultValue = "0") @Min(0) @Max(MAX_PAGE) int page,
            @Parameter(description = "Page size, 1..50") @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @Parameter(description = "newest | name | price_asc | price_desc",
                    schema = @Schema(allowableValues = {"newest", "name", "price_asc", "price_desc"}, defaultValue = "newest"))
            @RequestParam(defaultValue = "newest")
            @Pattern(regexp = ProductSort.PARAM_PATTERN, message = "must be one of: newest, name, price_asc, price_desc")
            String sort) {
        return productService.listProducts(category, featured, page, size, ProductSort.fromParam(sort));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get active product details by slug")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND: missing or inactive",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ProductDetailsResponse getProduct(@PathVariable String slug) {
        return productService.getProduct(slug);
    }
}
