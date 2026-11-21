package com.clothingstore.clothing_store_be;

import com.clothingstore.clothing_store_be.constants.LogMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

import java.util.Arrays;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class ClothingStoreBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClothingStoreBeApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logSuccessfulStartup(ApplicationReadyEvent applicationReadyEvent) {
		log.info(LogMessages.APPLICATION_STARTED,
				Arrays.toString(applicationReadyEvent.getApplicationContext().getEnvironment().getActiveProfiles()));
	}

}
