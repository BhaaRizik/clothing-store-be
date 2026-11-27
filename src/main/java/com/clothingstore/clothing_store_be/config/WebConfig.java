package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.constants.CartConstants;
import com.clothingstore.clothing_store_be.exception.common.ProblemDetailFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
@RequiredArgsConstructor
@Import(ProblemDetailFactory.class)
@EnableConfigurationProperties({CorsProperties.class, CatalogPaginationProperties.class, CatalogSlugProperties.class,
        CartProperties.class, ErrorProperties.class})
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Bean
    JsonMapperBuilderCustomizer strictIntegerInputCustomizer() {
        return builder -> builder.withCoercionConfig(LogicalType.Integer, coercionConfig -> coercionConfig
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = corsProperties.allowedOrigins().toArray(String[]::new);
        String[] allowedHeaders = {HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, CartConstants.CART_ID_HEADER};
        registry.addMapping(CartConstants.CART_CORS_PATH_PATTERN)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "HEAD", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(allowedHeaders)
                .allowCredentials(false)
                .maxAge(3600);
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowedHeaders(allowedHeaders)
                .allowCredentials(false)
                .maxAge(3600);
    }
}
