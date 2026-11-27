package com.clothingstore.clothing_store_be.controller.cart;

import com.clothingstore.clothing_store_be.dto.cart.AddCartItemRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartItemResponse;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.dto.common.ImageResponse;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.dto.product.ProductColorResponse;
import com.clothingstore.clothing_store_be.exception.cart.CartConflictException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemLimitExceededException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.CartNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.InsufficientStockException;
import com.clothingstore.clothing_store_be.exception.cart.VariantUnavailableException;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import com.clothingstore.clothing_store_be.service.cart.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(CartController.class)
class CartControllerTest {

    private static final String CART_ID = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c";
    private static final String VARIANT_ID = "650000000000000000000c01";
    private static final CartItemResponse LINE = new CartItemResponse(VARIANT_ID, "650000000000000000000b01",
            "oxford-shirt", "Oxford Shirt", new ImageResponse("/img/blue.svg", "Blue"), "M",
            new ProductColorResponse("Blue", "#0000FF"), 2, new Money("40.00", "ILS"), new Money("50.00", "ILS"),
            new Money("80.00", "ILS"), CartItemStatus.AVAILABLE, false);
    private static final CartResponse CART = new CartResponse(CART_ID, List.of(LINE), 2, new Money("80.00", "ILS"), 10);
    private static final CartResponse EMPTY_CART = new CartResponse(CART_ID, List.of(), 0, new Money("0.00", "ILS"), 10);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    private static void expectProblem(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void createCartReturns201WithLocationAndEmptyCart() throws Exception {
        when(cartService.createCart()).thenReturn(EMPTY_CART);

        mockMvc.perform(post("/api/v1/cart"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/cart"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(CART_ID))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.itemCount").value(0))
                .andExpect(jsonPath("$.subtotal.amount").value("0.00"))
                .andExpect(jsonPath("$.subtotal.currency").value("ILS"))
                .andExpect(jsonPath("$.maxItemQuantity").value(10));
    }

    @Test
    void getCartReturnsFullContract() throws Exception {
        when(cartService.getCart(CART_ID)).thenReturn(CART);

        mockMvc.perform(get("/api/v1/cart").header("X-Cart-Id", CART_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(CART_ID))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.subtotal.amount").value("80.00"))
                .andExpect(jsonPath("$.maxItemQuantity").value(10))
                .andExpect(jsonPath("$.items[0].variantId").value(VARIANT_ID))
                .andExpect(jsonPath("$.items[0].productId").value("650000000000000000000b01"))
                .andExpect(jsonPath("$.items[0].productSlug").value("oxford-shirt"))
                .andExpect(jsonPath("$.items[0].productName").value("Oxford Shirt"))
                .andExpect(jsonPath("$.items[0].image.url").value("/img/blue.svg"))
                .andExpect(jsonPath("$.items[0].size").value("M"))
                .andExpect(jsonPath("$.items[0].color.name").value("Blue"))
                .andExpect(jsonPath("$.items[0].color.hexCode").value("#0000FF"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice.amount").value("40.00"))
                .andExpect(jsonPath("$.items[0].originalPrice.amount").value("50.00"))
                .andExpect(jsonPath("$.items[0].lineTotal.amount").value("80.00"))
                .andExpect(jsonPath("$.items[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[0].priceChanged").value(false));
    }

    @Test
    void missingCartIdHeaderReturns400ValidationErrorOnHeaderField() throws Exception {
        expectProblem(mockMvc.perform(get("/api/v1/cart")), 400, "VALIDATION_ERROR");
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(jsonPath("$.errors[0].field").value("X-Cart-Id"));
        verifyNoInteractions(cartService);
    }

    @Test
    void missingCartIdHeaderOnWriteEndpointsReturns400() throws Exception {
        expectProblem(mockMvc.perform(post("/api/v1/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":1}")), 400, "VALIDATION_ERROR");
        expectProblem(mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}")), 400, "VALIDATION_ERROR");
        expectProblem(mockMvc.perform(delete("/api/v1/cart/items/" + VARIANT_ID)), 400, "VALIDATION_ERROR");
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "3f2b8c1e-9a4d-4e7b-8c2a-000000000000"})
    void malformedOrUnknownCartIdReturnsSame404(String cartId) throws Exception {
        when(cartService.getCart(cartId)).thenThrow(new CartNotFoundException());

        expectProblem(mockMvc.perform(get("/api/v1/cart").header("X-Cart-Id", cartId)), 404, "CART_NOT_FOUND");
        mockMvc.perform(get("/api/v1/cart").header("X-Cart-Id", cartId))
                .andExpect(jsonPath("$.detail").value("Cart not found."));
    }

    @Test
    void addItemPassesRequestAndIgnoresClientPrices() throws Exception {
        when(cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 2))).thenReturn(CART);

        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":2,\"price\":\"0.01\",\"total\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice.amount").value("40.00"));

        verify(cartService).addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"variantId\":\"\",\"quantity\":1}",
            "{\"variantId\":\"not-an-id\",\"quantity\":1}",
            "{\"variantId\":12345,\"quantity\":1}",
            "{\"quantity\":1}",
            "{\"variantId\":\"650000000000000000000c01\"}",
            "{\"variantId\":\"650000000000000000000c01\",\"quantity\":0}",
            "{\"variantId\":\"650000000000000000000c01\",\"quantity\":-1}",
            "{\"variantId\":\"650000000000000000000c01\",\"quantity\":11}",
            "{\"variantId\":\"650000000000000000000c01\",\"quantity\":\"many\"}",
            "not json",
            ""})
    void invalidAddRequestReturns400ValidationError(String body) throws Exception {
        expectProblem(mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "VALIDATION_ERROR");
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-an-id", "650000000000000000000c0", "650000000000000000000c011",
            "65000000000000000000zz01", "{$ne: null}", "<script>alert(1)</script>"})
    void malformedBodyVariantIdReturns400OnVariantIdFieldWithoutEchoingInput(String variantId) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":\"" + variantId + "\",\"quantity\":1}"));

        expectProblem(result, 400, "VALIDATION_ERROR");
        result.andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("variantId"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a valid product variant id"))
                .andExpect(content().string(not(containsString("not-an-id"))))
                .andExpect(content().string(not(containsString("script"))))
                .andExpect(content().string(not(containsString("$ne"))));
        verifyNoInteractions(cartService);
    }

    @Test
    void uppercaseHexVariantIdIsWellFormed() throws Exception {
        String uppercaseVariantId = VARIANT_ID.toUpperCase();
        when(cartService.addCartItem(CART_ID, new AddCartItemRequest(uppercaseVariantId, 1))).thenReturn(CART);

        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + uppercaseVariantId + "\",\"quantity\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void missingVariantIdIsReportedOnVariantIdField() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("variantId"));
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"variantId\":\"not-an-id\",\"quantity\":0}",
            "{\"variantId\":\"not-an-id\",\"quantity\":11}",
            "{\"variantId\":\"not-an-id\"}",
            "{\"quantity\":0}",
            "{}"})
    void everyInvalidAddFieldIsListed(String body) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[1].field").value("variantId"));
        verifyNoInteractions(cartService);
    }

    @Test
    void emptyOrMalformedJsonBodyReturns400WithoutInternals() throws Exception {
        for (String body : List.of("", "{", "{\"variantId\":", "[]", "null", "\"text\"")) {
            ResultActions added = mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            expectProblem(added, 400, "VALIDATION_ERROR");
            added.andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("jackson"))));
            ResultActions patched = mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            expectProblem(patched, 400, "VALIDATION_ERROR");
            patched.andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("jackson"))));
        }
        verifyNoInteractions(cartService);
    }

    @Test
    void clientPriceAndTotalFieldsAreIgnored() throws Exception {
        when(cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 2))).thenReturn(CART);

        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":2,\"price\":\"0.01\","
                                + "\"unitPrice\":{\"amount\":\"0.01\",\"currency\":\"ILS\"},\"subtotal\":0,"
                                + "\"lineTotal\":\"0.02\",\"unitPriceSnapshot\":0.01,\"productId\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice.amount").value("40.00"))
                .andExpect(jsonPath("$.subtotal.amount").value("80.00"));

        verify(cartService).addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 2));
    }

    @Test
    void quantityAboveConfiguredMaximumNamesQuantityField() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity must be between 1 and 10."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "11"})
    void quantityOutsideRangeUsesFriendlyMessageForAddAndPatch(String quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity must be between 1 and 10."));
        mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity must be between 1 and 10."));
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "\"2\"", "true", "null"})
    void nonIntegerOrNullQuantityIsRejectedOnQuantityFieldForAddAndPatch(String quantity) throws Exception {
        expectProblem(mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":" + quantity + "}")),
                400, "VALIDATION_ERROR");
        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":" + quantity + "}"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        expectProblem(mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}")),
                400, "VALIDATION_ERROR");
        mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        verifyNoInteractions(cartService);
    }

    @Test
    void nonIntegerQuantityNamesExpectedType() throws Exception {
        mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1.5}"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a valid integer"));
    }

    @Test
    void configuredMaximumQuantityIsAccepted() throws Exception {
        when(cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 10))).thenReturn(CART);

        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void addItemConflictsMapTo409WithCodes() throws Exception {
        String body = "{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":1}";
        when(cartService.addCartItem(anyString(), any(AddCartItemRequest.class)))
                .thenThrow(new VariantUnavailableException())
                .thenThrow(new InsufficientStockException())
                .thenThrow(new CartConflictException(new OptimisticLockingFailureException("stale")))
                .thenThrow(new CartNotFoundException());

        for (String code : List.of("VARIANT_UNAVAILABLE", "INSUFFICIENT_STOCK", "CART_CONFLICT")) {
            ResultActions result = mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            expectProblem(result, 409, code);
            result.andExpect(jsonPath("$.title").value("Conflict"))
                    .andExpect(content().string(not(containsString("Exception"))));
        }
        expectProblem(mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body)), 404, "CART_NOT_FOUND");
    }

    @Test
    void itemLimitConflictIncludesMaxItemQuantity() throws Exception {
        when(cartService.addCartItem(anyString(), any(AddCartItemRequest.class)))
                .thenThrow(new CartItemLimitExceededException(10));

        ResultActions result = mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":5}"));

        expectProblem(result, 409, "CART_ITEM_LIMIT_EXCEEDED");
        result.andExpect(jsonPath("$.maxItemQuantity").value(10));
    }

    @Test
    void insufficientStockMessageDoesNotRevealStock() throws Exception {
        when(cartService.addCartItem(anyString(), any(AddCartItemRequest.class)))
                .thenThrow(new InsufficientStockException());

        mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"variantId\":\"" + VARIANT_ID + "\",\"quantity\":5}"))
                .andExpect(jsonPath("$.detail").value("Not enough stock is available for the requested quantity."));
    }

    @Test
    void patchSetsQuantity() throws Exception {
        when(cartService.updateCartItemQuantity(CART_ID, VARIANT_ID, new UpdateCartItemRequest(3))).thenReturn(CART);

        mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(CART_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"quantity\":0}", "{\"quantity\":11}", "{\"quantity\":null}"})
    void invalidPatchRequestReturns400(String body) throws Exception {
        expectProblem(mockMvc.perform(patch("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "VALIDATION_ERROR");
        verifyNoInteractions(cartService);
    }

    @Test
    void patchOfUnknownLineReturns404CartItemNotFound() throws Exception {
        when(cartService.updateCartItemQuantity(anyString(), anyString(), any(UpdateCartItemRequest.class)))
                .thenThrow(new CartItemNotFoundException());

        expectProblem(mockMvc.perform(patch("/api/v1/cart/items/not-an-object-id").header("X-Cart-Id", CART_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}")), 404, "CART_ITEM_NOT_FOUND");
    }

    @Test
    void deleteRemovesLine() throws Exception {
        when(cartService.removeCartItem(CART_ID, VARIANT_ID)).thenReturn(EMPTY_CART);

        mockMvc.perform(delete("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.subtotal.amount").value("0.00"));
    }

    @Test
    void deleteOfUnknownLineReturns404AndConflictReturns409() throws Exception {
        when(cartService.removeCartItem(CART_ID, VARIANT_ID))
                .thenThrow(new CartItemNotFoundException())
                .thenThrow(new CartConflictException(new OptimisticLockingFailureException("stale")));

        expectProblem(mockMvc.perform(delete("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)),
                404, "CART_ITEM_NOT_FOUND");
        expectProblem(mockMvc.perform(delete("/api/v1/cart/items/" + VARIANT_ID).header("X-Cart-Id", CART_ID)),
                409, "CART_CONFLICT");
    }
}
