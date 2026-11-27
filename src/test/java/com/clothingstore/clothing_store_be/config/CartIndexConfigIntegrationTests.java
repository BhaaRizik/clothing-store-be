package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.TestcontainersConfiguration;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.cart.max-item-quantity=10", "app.cart.inactive-cart-expiry=30d"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CartIndexConfigIntegrationTests {

    private static final String CARTS = "carts";
    private static final String TTL_INDEX = "ttl_carts_updated_at";

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void resetCartIndexes() {
        if (!mongoTemplate.collectionExists(CARTS)) {
            mongoTemplate.createCollection(CARTS);
        }
        mongoTemplate.getCollection(CARTS).dropIndexes();
    }

    private Optional<Document> ttlIndex() {
        return mongoTemplate.getCollection(CARTS).listIndexes().into(new ArrayList<>()).stream()
                .filter(index -> TTL_INDEX.equals(index.getString("name")))
                .findFirst();
    }

    private static long expireAfterSeconds(Document index) {
        return ((Number) index.get("expireAfterSeconds")).longValue();
    }

    private void ensureIndexWithExpiry(Duration expiry) {
        new CartIndexConfig(mongoTemplate, new CartProperties(10, expiry)).ensureInactiveCartExpiryIndex();
    }

    @Test
    void missingIndexIsCreatedOnUpdatedAtWithConfiguredExpiry() {
        ensureIndexWithExpiry(Duration.ofDays(30));

        assertThat(ttlIndex()).hasValueSatisfying(index -> {
            assertThat(index.get("key", Document.class)).isEqualTo(new Document("updatedAt", 1));
            assertThat(expireAfterSeconds(index)).isEqualTo(Duration.ofDays(30).toSeconds());
        });
    }

    @Test
    void existingIndexIsUpdatedWhenConfiguredExpiryChanges() {
        ensureIndexWithExpiry(Duration.ofDays(30));

        ensureIndexWithExpiry(Duration.ofDays(7));

        assertThat(ttlIndex()).hasValueSatisfying(index ->
                assertThat(expireAfterSeconds(index)).isEqualTo(Duration.ofDays(7).toSeconds()));
    }

    @Test
    void existingIndexWithSameExpiryIsLeftUnchanged() {
        ensureIndexWithExpiry(Duration.ofHours(12));
        Document before = ttlIndex().orElseThrow();

        ensureIndexWithExpiry(Duration.ofHours(12));

        assertThat(ttlIndex()).contains(before);
    }

    @Test
    void indexCreatedWithoutExpiryByAnotherProcessGainsConfiguredExpiry() {
        mongoTemplate.indexOps(CARTS).createIndex(new Index().on("updatedAt", Sort.Direction.ASC).named(TTL_INDEX));

        ensureIndexWithExpiry(Duration.ofDays(30));

        List<Document> ttlIndexes = mongoTemplate.getCollection(CARTS).listIndexes().into(new ArrayList<>()).stream()
                .filter(index -> TTL_INDEX.equals(index.getString("name")))
                .toList();
        assertThat(ttlIndexes).singleElement().satisfies(index ->
                assertThat(index.get("expireAfterSeconds")).isNotNull());
    }
}
