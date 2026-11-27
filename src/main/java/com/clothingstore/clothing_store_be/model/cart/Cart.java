package com.clothingstore.clothing_store_be.model.cart;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("carts")
public record Cart(
        @Id String id,
        List<CartItem> items,
        Instant createdAt,
        Instant updatedAt,
        @Version Long version) {
}
