package com.clothingstore.clothing_store_be.controller.promotion;

import com.clothingstore.clothing_store_be.dto.promotion.PromotionResponse;
import com.clothingstore.clothing_store_be.service.promotion.PromotionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(PromotionController.class)
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionService promotionService;

    @Test
    void listsPromotions() throws Exception {
        when(promotionService.listActivePromotions()).thenReturn(List.of(
                new PromotionResponse("p1", "New season arrivals", "The new collection",
                        "/images/promotions/new-season.svg", "Coats", "Shop women", "/categories/women"),
                new PromotionResponse("p2", "Accessories edit", null,
                        "/images/promotions/accessories-edit.svg", "Scarf", null, null)));

        mockMvc.perform(get("/api/v1/promotions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("p1"))
                .andExpect(jsonPath("$[0].title").value("New season arrivals"))
                .andExpect(jsonPath("$[0].subtitle").value("The new collection"))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/promotions/new-season.svg"))
                .andExpect(jsonPath("$[0].imageAlt").value("Coats"))
                .andExpect(jsonPath("$[0].ctaLabel").value("Shop women"))
                .andExpect(jsonPath("$[0].ctaUrl").value("/categories/women"))
                .andExpect(jsonPath("$[1].subtitle").isEmpty())
                .andExpect(jsonPath("$[1].ctaLabel").isEmpty())
                .andExpect(jsonPath("$[1].ctaUrl").isEmpty());
    }

    @Test
    void returnsEmptyArrayWhenNoPromotionsAreActive() throws Exception {
        when(promotionService.listActivePromotions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/promotions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void unexpectedErrorReturnsGeneric500WithoutInternals() throws Exception {
        when(promotionService.listActivePromotions()).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/v1/promotions"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void ignoresUnknownQueryParameters() throws Exception {
        when(promotionService.listActivePromotions()).thenReturn(List.of(
                new PromotionResponse("p1", "New season arrivals", null,
                        "/images/promotions/new-season.svg", "Coats", null, null)));

        mockMvc.perform(get("/api/v1/promotions")
                        .param("active", "false")
                        .param("includeInactive", "true")
                        .param("size", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("p1"));
    }

    @Test
    void headRequestIsSupported() throws Exception {
        when(promotionService.listActivePromotions()).thenReturn(List.of());

        mockMvc.perform(head("/api/v1/promotions"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void writeMethodsReturn405ProblemDetailWithoutInvokingService(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/v1/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Injected\",\"active\":true}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ALLOW, not(containsString(method))));

        verify(promotionService, never()).listActivePromotions();
    }

    @Test
    void serializesNullOptionalFieldsAsExplicitNulls() throws Exception {
        when(promotionService.listActivePromotions()).thenReturn(List.of(
                new PromotionResponse("p2", "Accessories edit", null,
                        "/images/promotions/accessories-edit.svg", "Scarf", null, null)));

        mockMvc.perform(get("/api/v1/promotions"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":"p2","title":"Accessories edit","subtitle":null,
                          "imageUrl":"/images/promotions/accessories-edit.svg","imageAlt":"Scarf",
                          "ctaLabel":null,"ctaUrl":null}]
                        """, JsonCompareMode.STRICT));
    }
}
