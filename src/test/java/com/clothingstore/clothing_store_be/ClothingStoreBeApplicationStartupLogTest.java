package com.clothingstore.clothing_store_be;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ClothingStoreBeApplicationStartupLogTest {

    @Test
    void logsSuccessMessageWithActiveProfilesWhenApplicationIsReady(CapturedOutput output) {
        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {
            applicationContext.getEnvironment().setActiveProfiles("local");
            ApplicationReadyEvent applicationReadyEvent = new ApplicationReadyEvent(
                    new SpringApplication(ClothingStoreBeApplication.class), new String[0], applicationContext, Duration.ZERO);

            new ClothingStoreBeApplication().logSuccessfulStartup(applicationReadyEvent);
        }

        assertThat(output).contains("Clothing Store backend started successfully with active profiles: [local]");
    }
}
