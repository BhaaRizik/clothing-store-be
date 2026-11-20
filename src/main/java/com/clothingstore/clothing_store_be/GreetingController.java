package com.clothingstore.clothing_store_be;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GreetingController {

    @GetMapping("/greeting")
    public Map<String, String> greeting() {
        return Map.of("message", "Hello from Clothing Store API!");
    }
}
