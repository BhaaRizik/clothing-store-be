package com.clothingstore.clothing_store_be;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RequiredArgsConstructor
public class CartApiTestClient {

    private static final String CART_PATH = "/api/v1/cart";
    private static final String CART_ID_HEADER = "X-Cart-Id";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final int port;

    public String createCart() {
        return send("POST", CART_PATH, null, null).read("$.id");
    }

    public CartApiTestResponse getCart(String cartId) {
        return send("GET", CART_PATH, null, cartId);
    }

    public CartApiTestResponse addItem(String cartId, String variantId, int quantity) {
        return addItemWithBody(cartId, "{\"variantId\":\"" + variantId + "\",\"quantity\":" + quantity + "}");
    }

    public CartApiTestResponse addItemWithBody(String cartId, String body) {
        return send("POST", CART_PATH + "/items", body, cartId);
    }

    public CartApiTestResponse updateQuantity(String cartId, String variantId, int quantity) {
        return updateQuantityWithBody(cartId, variantId, "{\"quantity\":" + quantity + "}");
    }

    public CartApiTestResponse updateQuantityWithBody(String cartId, String variantId, String body) {
        return send("PATCH", CART_PATH + "/items/" + variantId, body, cartId);
    }

    public CartApiTestResponse removeItem(String cartId, String variantId) {
        return send("DELETE", CART_PATH + "/items/" + variantId, null, cartId);
    }

    private CartApiTestResponse send(String method, String path, String body, String cartId) {
        return cartId == null
                ? sendWithHeaders(method, path, body)
                : sendWithHeaders(method, path, body, CART_ID_HEADER, cartId);
    }

    public CartApiTestResponse sendWithHeaders(String method, String path, String body, String... headerPairs) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        for (int index = 0; index < headerPairs.length; index += 2) {
            builder.header(headerPairs[index], headerPairs[index + 1]);
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new CartApiTestResponse(response.statusCode(), response.headers(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
