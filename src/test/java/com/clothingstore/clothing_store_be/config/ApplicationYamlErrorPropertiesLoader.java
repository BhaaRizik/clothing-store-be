package com.clothingstore.clothing_store_be.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class ApplicationYamlErrorPropertiesLoader {

    public ErrorProperties loadErrorProperties() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application.yaml", new ClassPathResource("application.yaml"));
            return new Binder(ConfigurationPropertySources.from(sources))
                    .bind("app.errors", ErrorProperties.class)
                    .get();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
