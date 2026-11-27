package com.clothingstore.clothing_store_be.controller.cart;

import com.clothingstore.clothing_store_be.constants.CartConstants;
import com.clothingstore.clothing_store_be.dto.cart.AddCartItemRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.service.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(CartConstants.CART_PATH)
@Tag(name = "Cart", description = "Guest shopping cart identified by the X-Cart-Id header")
public class CartController {

    private final CartService cartService;

    @PostMapping
    @Operation(summary = "Create an empty guest cart",
            description = "Returns the new cart with its id. Send the id in the X-Cart-Id header on later cart requests.")
    @ApiResponse(responseCode = "201", description = "Cart created",
            headers = @Header(name = HttpHeaders.LOCATION, description = CartConstants.CART_PATH,
                    schema = @Schema(type = "string")))
    public ResponseEntity<CartResponse> createCart() {
        return ResponseEntity.created(URI.create(CartConstants.CART_PATH))
                .cacheControl(CacheControl.noStore())
                .body(cartService.createCart());
    }

    @GetMapping
    @Operation(summary = "Get the cart with current prices, availability and subtotal")
    @ApiResponse(responseCode = "200", description = "Cart found")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: missing X-Cart-Id header",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "CART_NOT_FOUND: unknown, expired or malformed cart id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "Cart id") @RequestHeader(CartConstants.CART_ID_HEADER) String cartId) {
        return noStore(cartService.getCart(cartId));
    }

    @PostMapping(CartConstants.CART_ITEMS_PATH)
    @Operation(summary = "Add a product variant to the cart",
            description = "Adding a variant that is already in the cart increases the quantity of the same line. "
                    + "Prices always come from the catalog.")
    @ApiResponse(responseCode = "200", description = "Updated cart")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: missing header, missing or malformed JSON body, missing variantId or one that "
                    + "is not 24 hexadecimal characters, or quantity outside 1..${app.cart.max-item-quantity}; "
                    + "every invalid field is listed in errors",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "CART_NOT_FOUND",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "VARIANT_UNAVAILABLE (well-formed variantId that is unknown, inactive or out of stock), "
                    + "CART_ITEM_LIMIT_EXCEEDED (with maxItemQuantity), INSUFFICIENT_STOCK or CART_CONFLICT",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CartResponse> addCartItem(
            @Parameter(description = "Cart id") @RequestHeader(CartConstants.CART_ID_HEADER) String cartId,
            @Valid @RequestBody AddCartItemRequest request) {
        return noStore(cartService.addCartItem(cartId, request));
    }

    @PatchMapping(CartConstants.CART_ITEM_PATH)
    @Operation(summary = "Set the quantity of a cart line")
    @ApiResponse(responseCode = "200", description = "Updated cart")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: missing header, missing or malformed JSON body, "
                    + "or quantity outside 1..${app.cart.max-item-quantity}",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404",
            description = "CART_NOT_FOUND, or CART_ITEM_NOT_FOUND when the variantId is not in the cart or is not "
                    + "24 hexadecimal characters",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "VARIANT_UNAVAILABLE, INSUFFICIENT_STOCK or CART_CONFLICT",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @Parameter(description = "Cart id") @RequestHeader(CartConstants.CART_ID_HEADER) String cartId,
            @Parameter(description = "Product variant id of the cart line") @PathVariable String variantId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return noStore(cartService.updateCartItemQuantity(cartId, variantId, request));
    }

    @DeleteMapping(CartConstants.CART_ITEM_PATH)
    @Operation(summary = "Remove a line from the cart")
    @ApiResponse(responseCode = "200", description = "Updated cart")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: missing X-Cart-Id header",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404",
            description = "CART_NOT_FOUND, or CART_ITEM_NOT_FOUND when the variantId is not in the cart or is not "
                    + "24 hexadecimal characters",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "CART_CONFLICT",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CartResponse> removeCartItem(
            @Parameter(description = "Cart id") @RequestHeader(CartConstants.CART_ID_HEADER) String cartId,
            @Parameter(description = "Product variant id of the cart line") @PathVariable String variantId) {
        return noStore(cartService.removeCartItem(cartId, variantId));
    }

    private static ResponseEntity<CartResponse> noStore(CartResponse cartResponse) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cartResponse);
    }
}
