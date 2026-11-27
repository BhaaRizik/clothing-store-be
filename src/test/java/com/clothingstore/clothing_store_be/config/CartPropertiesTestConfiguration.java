package com.clothingstore.clothing_store_be.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CartProperties.class)
public class CartPropertiesTestConfiguration {
}
