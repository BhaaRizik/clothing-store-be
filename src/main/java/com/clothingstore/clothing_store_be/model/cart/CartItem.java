package com.clothingstore.clothing_store_be.model.cart;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

public record CartItem(
        @Field(targetType = FieldType.OBJECT_ID) String productId,
        @Field(targetType = FieldType.OBJECT_ID) String variantId,
        int quantity,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal unitPriceSnapshot,
        Instant addedAt) {
}
