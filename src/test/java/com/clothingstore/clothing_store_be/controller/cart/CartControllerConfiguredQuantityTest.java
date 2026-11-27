package com.clothingstore.clothing_store_be.controller.cart;

import com.clothingstore.clothing_store_be.dto.cart.AddCartItemRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.service.cart.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = CartController.class, properties = "app.cart.max-item-quantity=3")
class CartControllerConfiguredQuantityTest {

    private static final String CART_ID = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c";
    private static final String VARIANT_ID = "650000000000000000000c01";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void configuredMaximumIsAcceptedInclusive() throws Exception {
        when(cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 3)))
                .thenReturn(new CartResponse(CART_ID, List.of(), 3, new Money("0.00", "ILS"), 3));

        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    void quantityAboveConfiguredMaximumIsRejectedForAddAndPatch() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity must be between 1 and 3."));
        mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity must be between 1 and 3."));
        verifyNoInteractions(cartService);
    }
}
