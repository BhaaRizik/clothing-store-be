package com.clothingstore.clothing_store_be.product;

/** Image embedded in a product document ({@code images: [{url, alt}]}). */
public record ProductImage(String url, String alt) {
}
