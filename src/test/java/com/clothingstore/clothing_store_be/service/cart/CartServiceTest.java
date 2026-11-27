package com.clothingstore.clothing_store_be.service.cart;

import com.clothingstore.clothing_store_be.config.CartProperties;
import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.dto.cart.AddCartItemRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartItemResponse;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.dto.common.Money;
import com.clothingstore.clothing_store_be.exception.cart.CartConflictException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemLimitExceededException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.CartNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.InsufficientStockException;
import com.clothingstore.clothing_store_be.exception.cart.VariantUnavailableException;
import com.clothingstore.clothing_store_be.mapper.cart.CartMapperImpl;
import com.clothingstore.clothing_store_be.mapper.common.MoneyMapperImpl;
import com.clothingstore.clothing_store_be.mapper.product.ProductVariantMapperImpl;
import com.clothingstore.clothing_store_be.model.cart.Cart;
import com.clothingstore.clothing_store_be.model.cart.CartItem;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import com.clothingstore.clothing_store_be.repository.cart.CartRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-10-01T10:00:00Z");
    private static final String CART_ID = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c";
    private static final String PRODUCT_ID = "650000000000000000000b01";
    private static final String OTHER_PRODUCT_ID = "650000000000000000000b02";
    private static final String VARIANT_ID = "650000000000000000000c01";
    private static final String OTHER_VARIANT_ID = "650000000000000000000c02";

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;

    private CartService cartService;

    @BeforeEach
    void createService() {
        CartMapperImpl cartMapper = new CartMapperImpl(new MoneyMapperImpl(), new ProductVariantMapperImpl());
        cartService = new CartService(cartRepository, productRepository, productVariantRepository, cartMapper,
                new CartProperties(10, Duration.ofDays(30)), new StoreProperties("ILS"),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Product product(String id, String basePrice, String salePrice, boolean active) {
        return new Product(id, "slug-" + id, "Name " + id, "Desc", "cat-1", null,
                List.of(new ProductImage("/img/black.svg", "Black", "Black"), new ProductImage("/img/any.svg", "Any", null)),
                new BigDecimal(basePrice), Optional.ofNullable(salePrice).map(BigDecimal::new).orElse(null),
                active, false, EARLIER, EARLIER);
    }

    private static ProductVariant variant(String id, String productId, Integer stock, Boolean active) {
        return new ProductVariant(id, productId, "SKU-" + id, "M", new ProductColor("Black", "#000000"), stock, active, 1,
                EARLIER, EARLIER);
    }

    private static CartItem item(String productId, String variantId, int quantity, String snapshot, Instant addedAt) {
        return new CartItem(productId, variantId, quantity, new BigDecimal(snapshot), addedAt);
    }

    private static Cart cart(CartItem... items) {
        return new Cart(CART_ID, List.of(items), EARLIER, EARLIER, 3L);
    }

    private void givenCart(Cart cart) {
        when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));
    }

    private void givenVariantAndProduct(ProductVariant variant, Product product) {
        when(productVariantRepository.findById(variant.id())).thenReturn(Optional.of(variant));
        when(productRepository.findById(product.id())).thenReturn(Optional.of(product));
    }

    private void givenCatalog(List<Product> products, List<ProductVariant> variants) {
        when(productRepository.findAllById(any())).thenReturn(products);
        when(productVariantRepository.findAllById(any())).thenReturn(variants);
    }

    private void saveReturnsArgument() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Cart savedCart() {
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createCartSavesEmptyCartWithRandomUuidAndTimestamps() {
        saveReturnsArgument();

        CartResponse response = cartService.createCart();

        Cart saved = savedCart();
        assertThat(UUID.fromString(saved.id()).toString()).isEqualTo(saved.id());
        assertThat(saved.items()).isEmpty();
        assertThat(saved.createdAt()).isEqualTo(NOW);
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(saved.version()).isNull();
        assertThat(response).isEqualTo(new CartResponse(saved.id(), List.of(), 0, new Money("0.00", "ILS"), 10));
        verifyNoInteractions(productRepository, productVariantRepository);
    }

    @Test
    void createCartGeneratesDifferentIds() {
        saveReturnsArgument();

        assertThat(cartService.createCart().id()).isNotEqualTo(cartService.createCart().id());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "", "3F2B8C1E-9A4D-4E7B-8C2A-1D5E6F7A8B9C", "1-1-1-1-1", "{\"$ne\":null}"})
    void malformedCartIdIsNotFoundWithoutQuerying(String cartId) {
        assertThatThrownBy(() -> cartService.getCart(cartId)).isInstanceOf(CartNotFoundException.class);
        verifyNoInteractions(cartRepository);
    }

    @Test
    void nullCartIdIsNotFound() {
        assertThatThrownBy(() -> cartService.getCart(null)).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void unknownCartIdIsNotFound() {
        when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(CART_ID)).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void addNewVariantCreatesLineWithCurrentPriceSnapshot() {
        Product product = product(PRODUCT_ID, "100.00", "80.00", true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 5, true);
        givenCart(cart());
        givenVariantAndProduct(variant, product);
        givenCatalog(List.of(product), List.of(variant));
        saveReturnsArgument();

        CartResponse response = cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 2));

        Cart saved = savedCart();
        assertThat(saved.items()).containsExactly(new CartItem(PRODUCT_ID, VARIANT_ID, 2, new BigDecimal("80.00"), NOW));
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(saved.createdAt()).isEqualTo(EARLIER);
        assertThat(saved.version()).isEqualTo(3L);
        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualTo(new Money("160.00", "ILS"));
        CartItemResponse line = response.items().getFirst();
        assertThat(line.unitPrice()).isEqualTo(new Money("80.00", "ILS"));
        assertThat(line.originalPrice()).isEqualTo(new Money("100.00", "ILS"));
        assertThat(line.lineTotal()).isEqualTo(new Money("160.00", "ILS"));
        assertThat(line.status()).isEqualTo(CartItemStatus.AVAILABLE);
        assertThat(line.priceChanged()).isFalse();
    }

    @Test
    void addExistingVariantMergesQuantityKeepsAddedAtAndRefreshesSnapshot() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 10, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 3, "45.00", EARLIER)));
        givenVariantAndProduct(variant, product);
        givenCatalog(List.of(product), List.of(variant));
        saveReturnsArgument();

        CartResponse response = cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 4));

        assertThat(savedCart().items())
                .containsExactly(new CartItem(PRODUCT_ID, VARIANT_ID, 7, new BigDecimal("50.00"), EARLIER));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(7);
        assertThat(response.items().getFirst().priceChanged()).isFalse();
    }

    @Test
    void mergeAllowsExactlyTheConfiguredMaximum() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 20, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 6, "50.00", EARLIER)));
        givenVariantAndProduct(variant, product);
        givenCatalog(List.of(product), List.of(variant));
        saveReturnsArgument();

        assertThat(cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 4)).itemCount()).isEqualTo(10);
    }

    @Test
    void mergeAboveConfiguredMaximumIsRejectedWithLimit() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 20, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 7, "50.00", EARLIER)));
        givenVariantAndProduct(variant, product);

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 4)))
                .isInstanceOfSatisfying(CartItemLimitExceededException.class, exception ->
                        assertThat(exception.getProblemProperties()).containsEntry("maxItemQuantity", 10));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void limitIsCheckedBeforeStock() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 2, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 8, "50.00", EARLIER)));
        givenVariantAndProduct(variant, product);

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 3)))
                .isInstanceOf(CartItemLimitExceededException.class);
    }

    @Test
    void addAboveStockIsRejectedWithoutRevealingStock() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 3, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 2, "50.00", EARLIER)));
        givenVariantAndProduct(variant, product);

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 2)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Not enough stock is available for the requested quantity.")
                .hasMessageNotContaining("3");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addUpToStockIsAccepted() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 3, true);
        givenCart(cart());
        givenVariantAndProduct(variant, product);
        givenCatalog(List.of(product), List.of(variant));
        saveReturnsArgument();

        assertThat(cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 3)).itemCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"0, true", "-2, true", "null, true", "5, false", "5, null"})
    void addOfOutOfStockOrInactiveVariantIsUnavailable(Integer stock, Boolean active) {
        givenCart(cart());
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant(VARIANT_ID, PRODUCT_ID, stock, active)));

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 1)))
                .isInstanceOf(VariantUnavailableException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addOfUnknownVariantIsUnavailable() {
        givenCart(cart());
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 1)))
                .isInstanceOf(VariantUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-object-id", "123", "{\"$ne\":null}", "zzzzzzzzzzzzzzzzzzzzzzzz"})
    void addOfMalformedVariantIdIsUnavailableWithoutQuerying(String variantId) {
        givenCart(cart());

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(variantId, 1)))
                .isInstanceOf(VariantUnavailableException.class);
        verifyNoInteractions(productVariantRepository);
    }

    @Test
    void addOfVariantWhoseProductIsInactiveIsUnavailable() {
        givenCart(cart());
        givenVariantAndProduct(variant(VARIANT_ID, PRODUCT_ID, 5, true), product(PRODUCT_ID, "10.00", null, false));

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 1)))
                .isInstanceOf(VariantUnavailableException.class);
    }

    @Test
    void addOfVariantWhoseProductIsMissingIsUnavailable() {
        givenCart(cart());
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant(VARIANT_ID, PRODUCT_ID, 5, true)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 1)))
                .isInstanceOf(VariantUnavailableException.class);
    }

    @Test
    void addToUnknownCartIsNotFound() {
        when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 1)))
                .isInstanceOf(CartNotFoundException.class);
        verifyNoInteractions(productVariantRepository);
    }

    @Test
    void optimisticLockFailureOnSaveBecomesCartConflict() {
        Product product = product(PRODUCT_ID, "50.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 5, true);
        givenCart(cart());
        givenVariantAndProduct(variant, product);
        when(cartRepository.save(any(Cart.class))).thenThrow(new OptimisticLockingFailureException("version mismatch"));

        assertThatThrownBy(() -> cartService.addCartItem(CART_ID, new AddCartItemRequest(VARIANT_ID, 1)))
                .isInstanceOf(CartConflictException.class)
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void patchSetsAbsoluteQuantityAndRefreshesSnapshot() {
        Product product = product(PRODUCT_ID, "60.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 4, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "55.00", EARLIER)));
        givenVariantAndProduct(variant, product);
        givenCatalog(List.of(product), List.of(variant));
        saveReturnsArgument();

        CartResponse response = cartService.updateCartItemQuantity(CART_ID, VARIANT_ID, new UpdateCartItemRequest(4));

        Cart saved = savedCart();
        assertThat(saved.items()).containsExactly(new CartItem(PRODUCT_ID, VARIANT_ID, 4, new BigDecimal("60.00"), EARLIER));
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(response.subtotal()).isEqualTo(new Money("240.00", "ILS"));
    }

    @Test
    void patchLoweringQuantityBelowStockIsAccepted() {
        Product product = product(PRODUCT_ID, "60.00", null, true);
        ProductVariant variant = variant(VARIANT_ID, PRODUCT_ID, 2, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 5, "60.00", EARLIER)));
        givenVariantAndProduct(variant, product);
        givenCatalog(List.of(product), List.of(variant));
        saveReturnsArgument();

        CartResponse response = cartService.updateCartItemQuantity(CART_ID, VARIANT_ID, new UpdateCartItemRequest(2));

        assertThat(response.items().getFirst().status()).isEqualTo(CartItemStatus.AVAILABLE);
    }

    @Test
    void patchAboveStockIsRejected() {
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "60.00", EARLIER)));
        givenVariantAndProduct(variant(VARIANT_ID, PRODUCT_ID, 2, true), product(PRODUCT_ID, "60.00", null, true));

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(CART_ID, VARIANT_ID, new UpdateCartItemRequest(3)))
                .isInstanceOf(InsufficientStockException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void patchOfLineWhoseVariantBecameInactiveIsUnavailable() {
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "60.00", EARLIER)));
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant(VARIANT_ID, PRODUCT_ID, 5, false)));

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(CART_ID, VARIANT_ID, new UpdateCartItemRequest(1)))
                .isInstanceOf(VariantUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {OTHER_VARIANT_ID, "not-an-object-id"})
    void patchOfVariantNotInCartIsItemNotFound(String variantId) {
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "60.00", EARLIER)));

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(CART_ID, variantId, new UpdateCartItemRequest(1)))
                .isInstanceOf(CartItemNotFoundException.class);
        verifyNoInteractions(productVariantRepository);
    }

    @Test
    void deleteRemovesOnlyTheMatchingLine() {
        Product product = product(PRODUCT_ID, "10.00", null, true);
        ProductVariant remaining = variant(OTHER_VARIANT_ID, PRODUCT_ID, 5, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER),
                item(PRODUCT_ID, OTHER_VARIANT_ID, 2, "10.00", EARLIER.plusSeconds(1))));
        givenCatalog(List.of(product), List.of(remaining));
        saveReturnsArgument();

        CartResponse response = cartService.removeCartItem(CART_ID, VARIANT_ID);

        Cart saved = savedCart();
        assertThat(saved.items()).extracting(CartItem::variantId).containsExactly(OTHER_VARIANT_ID);
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(response.itemCount()).isEqualTo(2);
    }

    @Test
    void deleteOfLastLineLeavesEmptyCartWithZeroSubtotal() {
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER)));
        saveReturnsArgument();

        CartResponse response = cartService.removeCartItem(CART_ID, VARIANT_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.subtotal()).isEqualTo(new Money("0.00", "ILS"));
    }

    @ParameterizedTest
    @ValueSource(strings = {OTHER_VARIANT_ID, "not-an-object-id"})
    void deleteOfVariantNotInCartIsItemNotFound(String variantId) {
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER)));

        assertThatThrownBy(() -> cartService.removeCartItem(CART_ID, variantId))
                .isInstanceOf(CartItemNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void deleteConflictBecomesCartConflict() {
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER)));
        when(cartRepository.save(any(Cart.class))).thenThrow(new OptimisticLockingFailureException("version mismatch"));

        assertThatThrownBy(() -> cartService.removeCartItem(CART_ID, VARIANT_ID)).isInstanceOf(CartConflictException.class);
    }

    @Test
    void getCartComputesStatusesSubtotalAndItemCountExcludingUnavailableLines() {
        String lowStockVariantId = "650000000000000000000c03";
        String outOfStockVariantId = "650000000000000000000c04";
        String inactiveProductVariantId = "650000000000000000000c05";
        String missingVariantId = "650000000000000000000c06";
        String missingProductId = "650000000000000000000b09";
        Product onSale = product(PRODUCT_ID, "100.00", "75.50", true);
        Product inactive = product(OTHER_PRODUCT_ID, "40.00", null, false);
        givenCart(cart(
                item(PRODUCT_ID, VARIANT_ID, 2, "75.50", EARLIER),
                item(PRODUCT_ID, lowStockVariantId, 3, "75.50", EARLIER.plusSeconds(1)),
                item(PRODUCT_ID, outOfStockVariantId, 1, "75.50", EARLIER.plusSeconds(2)),
                item(OTHER_PRODUCT_ID, inactiveProductVariantId, 1, "40.00", EARLIER.plusSeconds(3)),
                item(missingProductId, missingVariantId, 4, "9.99", EARLIER.plusSeconds(4))));
        givenCatalog(List.of(onSale, inactive), List.of(
                variant(VARIANT_ID, PRODUCT_ID, 5, true),
                variant(lowStockVariantId, PRODUCT_ID, 2, true),
                variant(outOfStockVariantId, PRODUCT_ID, 0, true),
                variant(inactiveProductVariantId, OTHER_PRODUCT_ID, 5, true)));

        CartResponse response = cartService.getCart(CART_ID);

        assertThat(response.items()).extracting(CartItemResponse::status).containsExactly(
                CartItemStatus.AVAILABLE, CartItemStatus.INSUFFICIENT_STOCK, CartItemStatus.UNAVAILABLE,
                CartItemStatus.UNAVAILABLE, CartItemStatus.UNAVAILABLE);
        assertThat(response.itemCount()).isEqualTo(11);
        assertThat(response.subtotal()).isEqualTo(new Money("377.50", "ILS"));
        assertThat(response.maxItemQuantity()).isEqualTo(10);
        CartItemResponse missing = response.items().get(4);
        assertThat(missing.productId()).isEqualTo(missingProductId);
        assertThat(missing.variantId()).isEqualTo(missingVariantId);
        assertThat(missing.productSlug()).isNull();
        assertThat(missing.productName()).isNull();
        assertThat(missing.image()).isNull();
        assertThat(missing.size()).isNull();
        assertThat(missing.color()).isNull();
        assertThat(missing.unitPrice()).isNull();
        assertThat(missing.originalPrice()).isNull();
        assertThat(missing.lineTotal()).isNull();
        assertThat(missing.priceChanged()).isFalse();
        assertThat(response.items().get(3).lineTotal()).isEqualTo(new Money("40.00", "ILS"));
    }

    @Test
    void getCartResolvesCatalogInBulk() {
        Product product = product(PRODUCT_ID, "10.00", null, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER),
                item(PRODUCT_ID, OTHER_VARIANT_ID, 1, "10.00", EARLIER)));
        givenCatalog(List.of(product), List.of(variant(VARIANT_ID, PRODUCT_ID, 5, true),
                variant(OTHER_VARIANT_ID, PRODUCT_ID, 5, true)));

        cartService.getCart(CART_ID);

        verify(productRepository).findAllById(List.of(PRODUCT_ID));
        verify(productVariantRepository).findAllById(List.of(VARIANT_ID, OTHER_VARIANT_ID));
        verify(productRepository, never()).findById(any());
        verify(productVariantRepository, never()).findById(any());
    }

    @Test
    void getCartOrdersItemsByAddedAt() {
        Product product = product(PRODUCT_ID, "10.00", null, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER.plusSeconds(60)),
                item(PRODUCT_ID, OTHER_VARIANT_ID, 1, "10.00", EARLIER)));
        givenCatalog(List.of(product), List.of(variant(VARIANT_ID, PRODUCT_ID, 5, true),
                variant(OTHER_VARIANT_ID, PRODUCT_ID, 5, true)));

        assertThat(cartService.getCart(CART_ID).items()).extracting(CartItemResponse::variantId)
                .containsExactly(OTHER_VARIANT_ID, VARIANT_ID);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "20.00, null, 20.00, false",
            "20.00, null, 20.0, false",
            "25.00, null, 20.00, true",
            "20.00, 18.00, 20.00, true",
            "20.00, 18.00, 18.00, false",
            "20.004, null, 20.00, false"})
    void priceChangedComparesCurrentUnitPriceWithSnapshotAtTwoDecimals(String basePrice, String salePrice,
            String snapshot, boolean expectedPriceChanged) {
        Product product = product(PRODUCT_ID, basePrice, salePrice, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, snapshot, EARLIER)));
        givenCatalog(List.of(product), List.of(variant(VARIANT_ID, PRODUCT_ID, 5, true)));

        assertThat(cartService.getCart(CART_ID).items().getFirst().priceChanged()).isEqualTo(expectedPriceChanged);
    }

    @Test
    void lineTotalUsesRoundedUnitPrice() {
        Product product = product(PRODUCT_ID, "10.005", null, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 3, "10.01", EARLIER)));
        givenCatalog(List.of(product), List.of(variant(VARIANT_ID, PRODUCT_ID, 5, true)));

        CartResponse response = cartService.getCart(CART_ID);

        assertThat(response.items().getFirst().unitPrice()).isEqualTo(new Money("10.01", "ILS"));
        assertThat(response.items().getFirst().lineTotal()).isEqualTo(new Money("30.03", "ILS"));
        assertThat(response.subtotal()).isEqualTo(new Money("30.03", "ILS"));
    }

    @Test
    void lineWithoutSaleHasNoOriginalPrice() {
        Product product = product(PRODUCT_ID, "10.00", null, true);
        givenCart(cart(item(PRODUCT_ID, VARIANT_ID, 1, "10.00", EARLIER)));
        givenCatalog(List.of(product), List.of(variant(VARIANT_ID, PRODUCT_ID, 5, true)));

        assertThat(cartService.getCart(CART_ID).items().getFirst().originalPrice()).isNull();
    }

    @Test
    void cartWithNullItemsIsTreatedAsEmpty() {
        givenCart(new Cart(CART_ID, null, EARLIER, EARLIER, 0L));

        CartResponse response = cartService.getCart(CART_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.itemCount()).isZero();
        assertThat(response.subtotal()).isEqualTo(new Money("0.00", "ILS"));
    }
}
