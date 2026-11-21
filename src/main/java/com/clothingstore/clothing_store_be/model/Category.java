package com.clothingstore.clothing_store_be.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

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
