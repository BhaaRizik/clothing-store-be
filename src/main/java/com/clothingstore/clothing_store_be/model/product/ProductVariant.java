package com.clothingstore.clothing_store_be.model.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Document("product_variants")
@CompoundIndex(name = "ix_product_variants_product_active_order", def = "{'productId': 1, 'active': 1, 'displayOrder': 1}")
public record ProductVariant(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String productId,
        @Indexed(name = "uk_product_variants_sku", unique = true) String sku,
        String size,
        ProductColor color,
        Integer stockQuantity,
        Boolean active,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt) {
}
