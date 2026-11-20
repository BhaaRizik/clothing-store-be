package com.clothingstore.clothing_store_be.product;

public record ImageResponse(String url, String alt) {

    static ImageResponse from(ProductImage image) {
        return new ImageResponse(image.url(), image.alt());
    }
}
