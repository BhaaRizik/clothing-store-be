package com.clothingstore.clothing_store_be;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ClothingStoreBeApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void demoGreetingEndpointIsRemoved() throws Exception {
		mockMvc.perform(get("/api/greeting"))
				.andExpect(status().isNotFound());
	}

	@Test
	void openApiDocumentDoesNotListGreetingAndResolvesConfiguredLimits() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/greeting']").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/products'].get.parameters[?(@.name == 'page')].description")
						.value("Zero-based page index, 0..10000"))
				.andExpect(jsonPath("$.paths['/api/v1/products'].get.parameters[?(@.name == 'size')].description")
						.value("Page size, 1..50"));
	}

	@Test
	void openApiDocumentIsServed() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/products']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categories/{slug}']").exists());
	}

	@Test
	void catalogIllustrationsReferencedBySeedDataAreServed() throws Exception {
		for (String file : new String[] {"men-classic-oxford-shirt.svg", "women-floral-midi-dress.svg",
				"shoes-chelsea-boots.svg", "acc-classic-sunglasses.svg", "category-men.svg", "category-accessories.svg"}) {
			mockMvc.perform(get("/images/catalog/" + file))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
		}
	}

	@Test
	void everyImageUrlInSeedScriptHasAMatchingStaticFile() throws Exception {
		String seed = Files.readString(Path.of("docker/mongo-init/seed.js"));
		Matcher m = Pattern.compile("/images/catalog/(category-[a-z]+)\\.svg").matcher(seed);
		int categories = 0;
		while (m.find()) {
			assertThat(Path.of("src/main/resources/static/images/catalog", m.group(1) + ".svg")).exists();
			categories++;
		}
		assertThat(categories).isEqualTo(4);
		Matcher products = Pattern.compile("images: img\\('([a-z0-9-]+)'").matcher(seed);
		int count = 0;
		while (products.find()) {
			assertThat(Path.of("src/main/resources/static/images/catalog", products.group(1) + ".svg")).exists();
			count++;
		}
		assertThat(count).isEqualTo(18);
	}

}
