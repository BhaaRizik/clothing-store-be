package com.clothingstore.clothing_store_be.controller.cart;

import com.clothingstore.clothing_store_be.config.ClockConfig;
import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.mapper.cart.CartMapper;
import com.clothingstore.clothing_store_be.model.cart.Cart;
import com.clothingstore.clothing_store_be.model.cart.CartItem;
import com.clothingstore.clothing_store_be.repository.cart.CartRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import com.clothingstore.clothing_store_be.service.cart.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(CartController.class)
@Import({CartService.class, ClockConfig.class})
@EnableConfigurationProperties(StoreProperties.class)
class CartControllerVariantIdValidationTest {

    private static final String CART_ID = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c";
    private static final Instant ADDED_AT = Instant.parse("2026-10-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductVariantRepository productVariantRepository;

    @MockitoBean
    private CartMapper cartMapper;

    @BeforeEach
    void givenCartWithOneLine() {
        CartItem item = new CartItem("650000000000000000000b01", "650000000000000000000c01", 1, new BigDecimal("10.00"),
                ADDED_AT);
        when(cartRepository.findById(CART_ID))
                .thenReturn(Optional.of(new Cart(CART_ID, List.of(item), ADDED_AT, ADDED_AT, 1L)));
    }

    private static void expectCartItemNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Cart item not found."));
    }

    @ParameterizedTest(name = "variantId={0}")
    @ValueSource(strings = {"not-an-id", "650000000000000000000c0", "650000000000000000000c011",
            "65000000000000000000zz01", "{$ne: null}", "650000000000000000000c01 "})
    void malformedPathVariantIdReturnsCartItemNotFoundWithoutWriting(String variantId) throws Exception {
        expectCartItemNotFound(mockMvc.perform(patch("/api/v1/cart/items/{variantId}", variantId)
                .header("X-Cart-Id", CART_ID).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}")));
        expectCartItemNotFound(mockMvc.perform(delete("/api/v1/cart/items/{variantId}", variantId)
                .header("X-Cart-Id", CART_ID)));

        verify(cartRepository, never()).save(any());
        verifyNoInteractions(productRepository, productVariantRepository, cartMapper);
    }
}
