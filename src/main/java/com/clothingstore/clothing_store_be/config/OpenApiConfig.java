package com.clothingstore.clothing_store_be.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI clothingStoreOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Clothing Store API")
                .version("v1")
                .description("Public storefront catalog and guest cart API. No endpoint requires authentication; "
                        + "cart endpoints identify the cart by the X-Cart-Id header."));
    }
}
