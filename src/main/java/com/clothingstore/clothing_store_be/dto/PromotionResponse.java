package com.clothingstore.clothing_store_be.dto;

public record PromotionResponse(
        String id,
        String title,
        String subtitle,
        String imageUrl,
        String imageAlt,
        String ctaLabel,
        String ctaUrl) {
}
