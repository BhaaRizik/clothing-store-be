package com.clothingstore.clothing_store_be.mapper.promotion;

import com.clothingstore.clothing_store_be.constants.PromotionConstants;
import com.clothingstore.clothing_store_be.dto.promotion.PromotionResponse;
import com.clothingstore.clothing_store_be.model.promotion.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    @Mapping(target = "ctaLabel", source = ".", qualifiedByName = "toSafeCtaLabel")
    @Mapping(target = "ctaUrl", source = ".", qualifiedByName = "toSafeCtaUrl")
    PromotionResponse toPromotionResponse(Promotion promotion);

    default List<PromotionResponse> toPromotionResponses(List<Promotion> promotions) {
        return Optional.ofNullable(promotions)
                .orElseGet(List::of)
                .stream()
                .map(this::toPromotionResponse)
                .toList();
    }

    @Named("toSafeCtaLabel")
    default String toSafeCtaLabel(Promotion promotion) {
        return hasSafeCallToAction(promotion) ? promotion.ctaLabel() : null;
    }

    @Named("toSafeCtaUrl")
    default String toSafeCtaUrl(Promotion promotion) {
        return hasSafeCallToAction(promotion) ? promotion.ctaUrl() : null;
    }

    default boolean hasSafeCallToAction(Promotion promotion) {
        boolean hasLabel = Optional.ofNullable(promotion.ctaLabel())
                .filter(label -> !label.isBlank())
                .isPresent();
        boolean hasInternalUrl = Optional.ofNullable(promotion.ctaUrl())
                .filter(url -> PromotionConstants.INTERNAL_CTA_PATH_PATTERN.matcher(url).matches())
                .isPresent();
        return hasLabel && hasInternalUrl;
    }
}
