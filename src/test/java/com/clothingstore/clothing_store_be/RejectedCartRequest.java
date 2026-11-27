package com.clothingstore.clothing_store_be;

public record RejectedCartRequest(String method, String path, String body, String cartId, int expectedStatus,
        String expectedCode) {
}
