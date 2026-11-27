package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.controller.cart.CartController;
import com.clothingstore.clothing_store_be.controller.category.CategoryController;
import com.clothingstore.clothing_store_be.exception.cart.InsufficientStockException;
import com.clothingstore.clothing_store_be.exception.category.CategoryNotFoundException;
import com.clothingstore.clothing_store_be.service.cart.CartService;
import com.clothingstore.clothing_store_be.service.category.CategoryService;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest({CategoryController.class, CartController.class})
class GlobalExceptionHandlerTest {

    private static final String CART_ID = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c";
    private static final String VALID_ADD_ITEM_BODY = "{\"variantId\":\"650000000000000000000c01\",\"quantity\":1}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CartService cartService;

    private static ResultActions expectProblem(ResultActions result, int status, String code, String title,
            String detail) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void unknownApiRouteReturns404RouteNotFoundWithoutEchoingPath() throws Exception {
        expectProblem(mockMvc.perform(get("/api/v1/does-not-exist")), 404, "ROUTE_NOT_FOUND",
                "Route not found", "The requested endpoint does not exist.")
                .andExpect(jsonPath("$.detail").value(not(containsString("does-not-exist"))));
    }

    @Test
    void unsupportedMethodReturns405MethodNotAllowedAndKeepsAllowHeader() throws Exception {
        expectProblem(mockMvc.perform(delete("/api/v1/categories")), 405, "METHOD_NOT_ALLOWED",
                "Method not allowed", "The HTTP method is not supported for this endpoint.")
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(content().string(not(containsString("DELETE"))));
    }

    @Test
    void putOnCollectionAlsoReturns405MethodNotAllowed() throws Exception {
        expectProblem(mockMvc.perform(put("/api/v1/categories")), 405, "METHOD_NOT_ALLOWED",
                "Method not allowed", "The HTTP method is not supported for this endpoint.");
    }

    @Test
    void unsupportedContentTypeReturns415UnsupportedMediaType() throws Exception {
        expectProblem(mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.TEXT_PLAIN).content(VALID_ADD_ITEM_BODY)),
                415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", "The request content type is not supported.")
                .andExpect(content().string(not(containsString("text/plain"))));
        verifyNoInteractions(cartService);
    }

    @Test
    void notAcceptableReturns406NotAcceptableAsProblemJsonForXmlOnlyClient() throws Exception {
        expectProblem(mockMvc.perform(get("/api/v1/categories").accept(MediaType.APPLICATION_XML)),
                406, "NOT_ACCEPTABLE", "Not acceptable", "The requested response media type is not supported.");
    }

    @Test
    void notAcceptableReturns406NotAcceptableAsProblemJsonForTextOnlyClient() throws Exception {
        expectProblem(mockMvc.perform(get("/api/v1/categories").accept(MediaType.TEXT_PLAIN)),
                406, "NOT_ACCEPTABLE", "Not acceptable", "The requested response media type is not supported.");
    }

    @Test
    void dataStoreFailureReturns503ServiceUnavailableWithoutInternals() throws Exception {
        when(categoryService.listActiveCategories()).thenThrow(
                new DataAccessResourceFailureException("mongodb://admin:secret@cluster0.example.net timed out"));

        expectProblem(mockMvc.perform(get("/api/v1/categories")), 503, "SERVICE_UNAVAILABLE",
                "Service unavailable", "The service is temporarily unavailable. Please try again later.")
                .andExpect(content().string(not(containsString("mongodb"))))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("DataAccessResourceFailureException"))));
    }

    @Test
    void unwrappedMongoTimeoutReturns503ServiceUnavailable() throws Exception {
        when(categoryService.listActiveCategories()).thenThrow(new MongoTimeoutException("cluster0 timed out"));

        expectProblem(mockMvc.perform(get("/api/v1/categories")), 503, "SERVICE_UNAVAILABLE",
                "Service unavailable", "The service is temporarily unavailable. Please try again later.")
                .andExpect(content().string(not(containsString("cluster0"))));
    }

    @Test
    void unwrappedMongoSocketFailureReturns503ServiceUnavailable() throws Exception {
        when(cartService.getCart(anyString())).thenThrow(
                new MongoSocketException("socket closed", new ServerAddress("cluster0.example.net")));

        expectProblem(mockMvc.perform(get("/api/v1/cart").header("X-Cart-Id", CART_ID)), 503,
                "SERVICE_UNAVAILABLE", "Service unavailable",
                "The service is temporarily unavailable. Please try again later.")
                .andExpect(content().string(not(containsString("cluster0"))));
    }

    @Test
    void unexpectedRuntimeExceptionReturns500WithoutMessageOrClassName() throws Exception {
        when(categoryService.listActiveCategories()).thenThrow(new RuntimeException("secret internal detail"));

        expectProblem(mockMvc.perform(get("/api/v1/categories")), 500, "INTERNAL_ERROR",
                "Internal server error", "An unexpected error occurred. Please try again later.")
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("java."))))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void validationErrorKeepsExisting400Contract() throws Exception {
        expectProblem(mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"variantId\":\"\",\"quantity\":1}")),
                400, "VALIDATION_ERROR", "Validation failed", "One or more request parameters are invalid.")
                .andExpect(jsonPath("$.errors[0].field").value("variantId"));
    }

    @Test
    void resourceNotFoundKeepsExisting404Contract() throws Exception {
        when(categoryService.getActiveCategory("archive")).thenThrow(new CategoryNotFoundException());

        expectProblem(mockMvc.perform(get("/api/v1/categories/archive")), 404, "CATEGORY_NOT_FOUND",
                "Resource not found", "Category not found.");
    }

    @Test
    void conflictKeepsExisting409Contract() throws Exception {
        when(cartService.addCartItem(anyString(), any())).thenThrow(new InsufficientStockException());

        expectProblem(mockMvc.perform(post("/api/v1/cart/items").header("X-Cart-Id", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ADD_ITEM_BODY)),
                409, "INSUFFICIENT_STOCK", "Conflict",
                "Not enough stock is available for the requested quantity.");
    }
}
