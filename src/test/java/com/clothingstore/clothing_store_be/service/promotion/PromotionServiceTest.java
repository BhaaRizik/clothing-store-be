package com.clothingstore.clothing_store_be.service.promotion;

import com.clothingstore.clothing_store_be.dto.promotion.PromotionResponse;
import com.clothingstore.clothing_store_be.mapper.promotion.PromotionMapperImpl;
import com.clothingstore.clothing_store_be.model.promotion.Promotion;
import com.clothingstore.clothing_store_be.repository.promotion.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Mock
    private PromotionRepository promotionRepository;

    private PromotionService promotionService;

    @BeforeEach
    void createService() {
        promotionService = new PromotionService(promotionRepository, new PromotionMapperImpl(),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private static Promotion promotion(String id, String title, String ctaUrl) {
        return new Promotion(id, title, null, "/images/promotions/" + id + ".svg", title, "Shop", ctaUrl, 1, true,
                null, null, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void queriesRepositoryAtClockTimeAndMapsInRepositoryOrder() {
        when(promotionRepository.findActivePromotionsAt(FIXED_NOW)).thenReturn(List.of(
                promotion("p1", "New season", "/categories/women"),
                promotion("p2", "Accessories edit", "https://evil.example")));

        List<PromotionResponse> result = promotionService.listActivePromotions();

        assertThat(result).extracting(PromotionResponse::id).containsExactly("p1", "p2");
        assertThat(result.getFirst().ctaUrl()).isEqualTo("/categories/women");
        assertThat(result.get(1).ctaUrl()).isNull();
        assertThat(result.get(1).ctaLabel()).isNull();
        verify(promotionRepository).findActivePromotionsAt(FIXED_NOW);
    }

    @Test
    void returnsEmptyListWhenNoPromotionsAreActive() {
        when(promotionRepository.findActivePromotionsAt(FIXED_NOW)).thenReturn(List.of());

        assertThat(promotionService.listActivePromotions()).isEmpty();
    }
}
