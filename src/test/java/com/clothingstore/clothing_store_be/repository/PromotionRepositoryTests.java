package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.TestcontainersConfiguration;
import com.clothingstore.clothing_store_be.model.Promotion;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PromotionRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private PromotionRepository promotionRepository;

    @BeforeEach
    void clearPromotions() {
        mongoTemplate.getCollection("promotions").deleteMany(new Document());
    }

    private void insertPromotion(String title, int displayOrder, boolean active, Instant startsAt, Instant endsAt) {
        Date created = Date.from(NOW.minus(Duration.ofDays(30)));
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", title).append("subtitle", null).append("imageUrl", "/images/promotions/x.svg")
                .append("imageAlt", "Alt").append("ctaLabel", null).append("ctaUrl", null)
                .append("displayOrder", displayOrder).append("active", active)
                .append("startsAt", startsAt == null ? null : Date.from(startsAt))
                .append("endsAt", endsAt == null ? null : Date.from(endsAt))
                .append("createdAt", created).append("updatedAt", created));
    }

    private void insertPromotionWithoutScheduleFields(String title, int displayOrder) {
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", title).append("imageUrl", "/images/promotions/x.svg").append("imageAlt", "Alt")
                .append("displayOrder", displayOrder).append("active", true));
    }

    private List<String> activeTitlesAtNow() {
        return promotionRepository.findActivePromotionsAt(NOW).stream().map(Promotion::title).toList();
    }

    @Test
    void excludesInactivePromotions() {
        insertPromotion("Live", 1, true, null, null);
        insertPromotion("Archived", 1, false, null, null);

        assertThat(activeTitlesAtNow()).containsExactly("Live");
    }

    @Test
    void excludesPromotionsThatHaveNotStartedYet() {
        insertPromotion("Upcoming", 1, true, NOW.plusSeconds(1), null);
        insertPromotion("Started", 2, true, NOW.minusSeconds(1), null);

        assertThat(activeTitlesAtNow()).containsExactly("Started");
    }

    @Test
    void includesPromotionStartingExactlyNow() {
        insertPromotion("Starts now", 1, true, NOW, null);

        assertThat(activeTitlesAtNow()).containsExactly("Starts now");
    }

    @Test
    void excludesPromotionsThatHaveEnded() {
        insertPromotion("Ended", 1, true, null, NOW.minusSeconds(1));
        insertPromotion("Running", 2, true, null, NOW.plusSeconds(1));

        assertThat(activeTitlesAtNow()).containsExactly("Running");
    }

    @Test
    void excludesPromotionEndingExactlyNow() {
        insertPromotion("Ends now", 1, true, null, NOW);

        assertThat(activeTitlesAtNow()).isEmpty();
    }

    @Test
    void includesPromotionsWithNullOrMissingScheduleDates() {
        insertPromotion("Null dates", 1, true, null, null);
        insertPromotionWithoutScheduleFields("Missing dates", 2);
        insertPromotion("Inside window", 3, true, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)));

        assertThat(activeTitlesAtNow()).containsExactly("Null dates", "Missing dates", "Inside window");
    }

    @Test
    void sortsByDisplayOrderThenTitle() {
        insertPromotion("Zeta", 2, true, null, null);
        insertPromotion("Beta", 1, true, null, null);
        insertPromotion("Alpha", 2, true, null, null);
        insertPromotion("Gamma", 1, true, null, null);

        assertThat(activeTitlesAtNow()).containsExactly("Beta", "Gamma", "Alpha", "Zeta");
    }

    @Test
    void mapsDocumentFieldsIncludingInstants() {
        Instant startsAt = NOW.minus(Duration.ofDays(1));
        insertPromotion("Mapped", 1, true, startsAt, null);

        Promotion promotion = promotionRepository.findActivePromotionsAt(NOW).getFirst();

        assertThat(promotion.id()).isNotBlank();
        assertThat(promotion.startsAt()).isEqualTo(startsAt);
        assertThat(promotion.endsAt()).isNull();
        assertThat(promotion.displayOrder()).isEqualTo(1);
        assertThat(promotion.active()).isTrue();
    }

    @Test
    void excludesDocumentWithoutActiveField() {
        insertPromotion("Live", 1, true, null, null);
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", "No active flag").append("imageUrl", "/images/promotions/x.svg")
                .append("imageAlt", "Alt").append("displayOrder", 0));

        assertThat(activeTitlesAtNow()).containsExactly("Live");
    }

    @Test
    void excludesDocumentWhoseActiveFlagIsNotBooleanTrue() {
        insertPromotion("Live", 1, true, null, null);
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", "String flag").append("imageUrl", "/images/promotions/x.svg")
                .append("imageAlt", "Alt").append("displayOrder", 0).append("active", "true"));
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", "Null flag").append("imageUrl", "/images/promotions/x.svg")
                .append("imageAlt", "Alt").append("displayOrder", 0).append("active", null));

        assertThat(activeTitlesAtNow()).containsExactly("Live");
    }

    @Test
    void excludesPromotionWithInvertedScheduleWindow() {
        insertPromotion("Inverted", 1, true, NOW.minus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(2)));
        insertPromotion("Valid", 2, true, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)));

        assertThat(activeTitlesAtNow()).containsExactly("Valid");
    }

    @Test
    void evaluatesScheduleAgainstTheGivenMoment() {
        insertPromotion("Spring sale", 1, true, NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(3)));

        assertThat(promotionRepository.findActivePromotionsAt(NOW)).isEmpty();
        assertThat(promotionRepository.findActivePromotionsAt(NOW.plus(Duration.ofDays(2))))
                .extracting(Promotion::title).containsExactly("Spring sale");
        assertThat(promotionRepository.findActivePromotionsAt(NOW.plus(Duration.ofDays(3)))).isEmpty();
    }

    @Test
    void mapsDocumentWithUnknownFieldsAndMissingOptionalFields() {
        mongoTemplate.getCollection("promotions").insertOne(new Document("_id", new ObjectId())
                .append("title", "Minimal").append("imageUrl", "/images/promotions/x.svg").append("imageAlt", "Alt")
                .append("displayOrder", 1).append("active", true)
                .append("internalNote", "do not expose").append("legacyPriority", 7));

        Promotion promotion = promotionRepository.findActivePromotionsAt(NOW).getFirst();

        assertThat(promotion.title()).isEqualTo("Minimal");
        assertThat(promotion.subtitle()).isNull();
        assertThat(promotion.ctaLabel()).isNull();
        assertThat(promotion.ctaUrl()).isNull();
        assertThat(promotion.createdAt()).isNull();
        assertThat(promotion.updatedAt()).isNull();
    }

    @Test
    void returnsEmptyListWhenCollectionIsEmpty() {
        assertThat(promotionRepository.findActivePromotionsAt(NOW)).isEmpty();
    }

    @Test
    void createsCompoundActiveDisplayOrderIndex() {
        assertThat(mongoTemplate.indexOps(Promotion.class).getIndexInfo())
                .anySatisfy(index -> assertThat(index.getName()).isEqualTo("ix_promotions_active_order"));
    }
}
