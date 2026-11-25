package com.clothingstore.clothing_store_be.controller.product;

import com.clothingstore.clothing_store_be.constants.ProductSortParameters;
import com.clothingstore.clothing_store_be.constants.ValidationConstants;
import com.clothingstore.clothing_store_be.dto.common.PageResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductCardResponse;
import com.clothingstore.clothing_store_be.dto.product.ProductDetailsResponse;
import com.clothingstore.clothing_store_be.mapper.product.ProductSortMapper;
import com.clothingstore.clothing_store_be.service.product.ProductService;
import com.clothingstore.clothing_store_be.validation.MaxConfiguredPage;
import com.clothingstore.clothing_store_be.validation.MaxConfiguredPageSize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public storefront products")
public class ProductController {

    private final ProductService productService;
    private final ProductSortMapper productSortMapper;

    @GetMapping
    @Operation(summary = "List active products as cards", description = "Paginated; optional category and featured filters.")
    @ApiResponse(responseCode = "200", description = "Page of product cards")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: invalid page (0..${app.catalog.pagination.max-page}), "
                    + "size (1..${app.catalog.pagination.max-page-size}), sort or featured",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND: unknown or inactive category slug",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<ProductCardResponse> listProducts(
            @Parameter(description = "Active category slug") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by featured flag") @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Zero-based page index, 0..${app.catalog.pagination.max-page}")
            @RequestParam(defaultValue = "0") @Min(0) @MaxConfiguredPage int page,
            @Parameter(description = "Page size, 1..${app.catalog.pagination.max-page-size}")
            @RequestParam(defaultValue = "${app.catalog.pagination.default-page-size}") @Min(1) @MaxConfiguredPageSize int size,
            @Parameter(description = "newest | name | price_asc | price_desc",
                    schema = @Schema(allowableValues = {ProductSortParameters.NEWEST, ProductSortParameters.NAME,
                            ProductSortParameters.PRICE_ASC, ProductSortParameters.PRICE_DESC},
                            defaultValue = ProductSortParameters.NEWEST))
            @RequestParam(defaultValue = ProductSortParameters.NEWEST)
            @Pattern(regexp = ValidationConstants.PRODUCT_SORT_PATTERN, message = ValidationConstants.PRODUCT_SORT_MESSAGE)
            String sort) {
        return productService.listProducts(category, featured, page, size, productSortMapper.toProductSort(sort));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get active product details by slug",
            description = "Includes the active variants ordered by displayOrder; available is true when the variant "
                    + "is in stock, stock counts are never exposed. Responds with Cache-Control: no-store.")
    @ApiResponse(responseCode = "200", description = "Product found, with variants (empty array when none)",
            headers = @Header(name = HttpHeaders.CACHE_CONTROL, description = "no-store",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND: missing or inactive",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ProductDetailsResponse> getProduct(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(productService.getProduct(slug));
    }
}
