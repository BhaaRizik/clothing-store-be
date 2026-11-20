package com.clothingstore.clothing_store_be;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple connectivity check endpoint. CORS is configured globally in {@code config.WebConfig}.
 */
@RestController
@RequestMapping("/api")
public class GreetingController {

    @GetMapping("/greeting")
    public Map<String, String> greeting() {
        return Map.of("message", "Hello from Clothing Store API!");
    }
}
