package com.clothingstore.clothing_store_be.controller.promotion;

import com.clothingstore.clothing_store_be.dto.promotion.PromotionResponse;
import com.clothingstore.clothing_store_be.service.promotion.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promotions", description = "Public storefront homepage promotions")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "List currently active promotions",
            description = "Active promotions whose schedule window contains the current time, sorted by displayOrder "
                    + "ascending, then title. ctaLabel and ctaUrl are null unless the CTA is an internal path.")
    @ApiResponse(responseCode = "200", description = "Active promotions, empty array when none")
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: unexpected failure",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public List<PromotionResponse> listPromotions() {
        return promotionService.listActivePromotions();
    }
}
