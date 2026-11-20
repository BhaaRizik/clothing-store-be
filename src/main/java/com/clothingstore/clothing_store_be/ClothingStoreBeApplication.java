package com.clothingstore.clothing_store_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClothingStoreBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClothingStoreBeApplication.class, args);
	}

}
