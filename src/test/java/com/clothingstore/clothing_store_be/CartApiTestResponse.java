package com.clothingstore.clothing_store_be;

import com.jayway.jsonpath.JsonPath;

import java.net.http.HttpHeaders;

public record CartApiTestResponse(int status, HttpHeaders headers, String body) {

    public <T> T read(String path) {
        return JsonPath.read(body, path);
    }

    public String header(String name) {
        return headers.firstValue(name).orElse(null);
    }
}
