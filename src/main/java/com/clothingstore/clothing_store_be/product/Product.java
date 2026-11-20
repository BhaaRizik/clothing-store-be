package com.clothingstore.clothing_store_be.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Persistent product in the {@code products} collection. Never returned from the API directly.
 * Index names must stay identical to those created by {@code docker/mongo-init/seed.js}.
 */
@Document("products")
@CompoundIndex(name = "ix_products_category_active_created", def = "{'categoryId': 1, 'active': 1, 'createdAt': -1}")
@CompoundIndex(name = "ix_products_featured_active", def = "{'featured': 1, 'active': 1}")
public record Product(
        @Id String id,
        @Indexed(name = "uk_products_slug", unique = true) String slug,
        String name,
        String description,
        @Field(targetType = FieldType.OBJECT_ID) String categoryId,
        String brand,
        List<ProductImage> images,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal basePrice,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal salePrice,
        boolean active,
        boolean featured,
        Instant createdAt,
        Instant updatedAt) {

    public Product {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
