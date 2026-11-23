package com.clothingstore.clothing_store_be.mapper;

import com.clothingstore.clothing_store_be.dto.PromotionResponse;
import com.clothingstore.clothing_store_be.model.Promotion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionMapperTest {

    private final PromotionMapper promotionMapper = new PromotionMapperImpl();

    private static Promotion promotionWithCallToAction(String ctaLabel, String ctaUrl) {
        return new Promotion("p1", "New season", "Fresh styles", "/images/promotions/new-season.svg", "Coats",
                ctaLabel, ctaUrl, 1, true, null, null, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void mapsAllFieldsAndKeepsInternalPathCallToAction() {
        PromotionResponse response = promotionMapper.toPromotionResponse(
                promotionWithCallToAction("Shop women", "/categories/women"));

        assertThat(response).isEqualTo(new PromotionResponse("p1", "New season", "Fresh styles",
                "/images/promotions/new-season.svg", "Coats", "Shop women", "/categories/women"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example/path", "http://shop.example/categories", "//evil.com",
            "/\\evil", "javascript:alert(1)", "categories/women", " /categories/women", "", "/\t/evil.com", "/\n/evil.com", "/categories women"})
    void dropsCallToActionWhenUrlIsNotAnInternalPath(String ctaUrl) {
        PromotionResponse response = promotionMapper.toPromotionResponse(
                promotionWithCallToAction("Shop now", ctaUrl));

        assertThat(response.ctaLabel()).isNull();
        assertThat(response.ctaUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/products?featured=true&sort=newest", "/categories/women#top", "/products/men-shirt"})
    void keepsCallToActionForInternalPathsWithQueryOrFragment(String ctaUrl) {
        PromotionResponse response = promotionMapper.toPromotionResponse(
                promotionWithCallToAction("Shop now", ctaUrl));

        assertThat(response.ctaLabel()).isEqualTo("Shop now");
        assertThat(response.ctaUrl()).isEqualTo(ctaUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/categories/women ", "/\u0000evil", "/categories\r\nSet-Cookie:x=1", "/\u007f",
            "data:text/html,<script>alert(1)</script>", "JAVASCRIPT:alert(1)", "\\evil.com", "/\\/evil.com"})
    void dropsCallToActionForControlCharactersAndNonPathSchemes(String ctaUrl) {
        PromotionResponse response = promotionMapper.toPromotionResponse(
                promotionWithCallToAction("Shop now", ctaUrl));

        assertThat(response.ctaLabel()).isNull();
        assertThat(response.ctaUrl()).isNull();
    }

    @Test
    void dropsCallToActionWhenBothLabelAndUrlAreMissing() {
        PromotionResponse response = promotionMapper.toPromotionResponse(promotionWithCallToAction(null, null));

        assertThat(response.ctaLabel()).isNull();
        assertThat(response.ctaUrl()).isNull();
        assertThat(response.title()).isEqualTo("New season");
        assertThat(response.imageAlt()).isEqualTo("Coats");
    }

    @Test
    void dropsCallToActionWhenUrlIsNull() {
        PromotionResponse response = promotionMapper.toPromotionResponse(promotionWithCallToAction("Shop now", null));

        assertThat(response.ctaLabel()).isNull();
        assertThat(response.ctaUrl()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void dropsCallToActionWhenLabelIsBlank(String ctaLabel) {
        PromotionResponse response = promotionMapper.toPromotionResponse(
                promotionWithCallToAction(ctaLabel, "/categories/women"));

        assertThat(response.ctaLabel()).isNull();
        assertThat(response.ctaUrl()).isNull();
    }

    @Test
    void nullSubtitlePassesThrough() {
        Promotion promotion = new Promotion("p2", "Accessories edit", null, "/images/promotions/accessories-edit.svg",
                "Scarf", null, null, 2, true, null, null, Instant.EPOCH, Instant.EPOCH);

        PromotionResponse response = promotionMapper.toPromotionResponse(promotion);

        assertThat(response.subtitle()).isNull();
        assertThat(response.title()).isEqualTo("Accessories edit");
    }

    @Test
    void mapsListsInOrderAndNullListToEmpty() {
        List<PromotionResponse> responses = promotionMapper.toPromotionResponses(List.of(
                promotionWithCallToAction("A", "/a"), promotionWithCallToAction("B", "/b")));

        assertThat(responses).extracting(PromotionResponse::ctaUrl).containsExactly("/a", "/b");
        assertThat(promotionMapper.toPromotionResponses(null)).isEmpty();
    }
}
