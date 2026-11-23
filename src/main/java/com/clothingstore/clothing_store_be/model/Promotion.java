package com.clothingstore.clothing_store_be.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("promotions")
@CompoundIndex(name = "ix_promotions_active_order", def = "{'active': 1, 'displayOrder': 1}")
public record Promotion(
        @Id String id,
        String title,
        String subtitle,
        String imageUrl,
        String imageAlt,
        String ctaLabel,
        String ctaUrl,
        int displayOrder,
        boolean active,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt) {
}
