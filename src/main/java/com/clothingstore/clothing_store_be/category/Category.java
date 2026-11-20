package com.clothingstore.clothing_store_be.category;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

/**
 * Persistent category in the {@code categories} collection. Never returned from the API directly.
 * Index names must stay identical to those created by {@code docker/mongo-init/seed.js}.
 */
@Document("categories")
public record Category(
        @Id String id,
        @Indexed(name = "uk_categories_slug", unique = true) String slug,
        String name,
        String description,
        @Field(targetType = FieldType.OBJECT_ID) String parentId,
        String imageUrl,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
