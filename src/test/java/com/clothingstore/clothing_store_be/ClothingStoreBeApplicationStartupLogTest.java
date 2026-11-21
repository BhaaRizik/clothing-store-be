package com.clothingstore.clothing_store_be;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(useMainMethod = UseMainMethod.ALWAYS)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ClothingStoreBeApplicationStartupLogTest {

    @Test
    void mainMethodLogsSuccessMessageWithActiveProfiles(CapturedOutput output) {
        assertThat(output).contains("Clothing Store backend started successfully with active profiles: [test]");
    }
}
