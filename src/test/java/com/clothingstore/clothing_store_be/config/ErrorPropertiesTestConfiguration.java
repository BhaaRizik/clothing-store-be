package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.exception.common.ProblemDetailFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(ProblemDetailFactory.class)
@EnableConfigurationProperties(ErrorProperties.class)
public class ErrorPropertiesTestConfiguration {
}
