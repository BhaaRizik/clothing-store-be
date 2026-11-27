package com.clothingstore.clothing_store_be.service.cart;

import com.clothingstore.clothing_store_be.config.CartProperties;
import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.constants.CartConstants;
import com.clothingstore.clothing_store_be.constants.LogMessages;
import com.clothingstore.clothing_store_be.dto.cart.AddCartItemRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.exception.cart.CartConflictException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemLimitExceededException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.CartNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.VariantUnavailableException;
import com.clothingstore.clothing_store_be.mapper.cart.CartMapper;
import com.clothingstore.clothing_store_be.model.cart.Cart;
import com.clothingstore.clothing_store_be.model.cart.CartItem;
import com.clothingstore.clothing_store_be.model.cart.CartItemStatus;
import com.clothingstore.clothing_store_be.model.cart.PricedCart;
import com.clothingstore.clothing_store_be.model.cart.PricedCartItem;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import com.clothingstore.clothing_store_be.repository.cart.CartRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductRepository;
import com.clothingstore.clothing_store_be.repository.product.ProductVariantRepository;
import com.clothingstore.clothing_store_be.util.CartServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartMapper cartMapper;
    private final CartProperties cartProperties;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public CartResponse createCart() {
        Instant now = clock.instant();
        Cart cart = saveCart(new Cart(UUID.randomUUID().toString(), List.of(), now, now, null));
        log.info(LogMessages.CART_CREATED);
        return toCartResponse(cart);
    }

    public CartResponse getCart(String cartId) {
        return toCartResponse(requireCart(cartId));
    }

    public CartResponse addCartItem(String cartId, AddCartItemRequest request) {
        Cart cart = requireCart(cartId);
        ProductVariant variant = requireAvailableVariant(request.variantId());
        Product product = requireAvailableProduct(variant);
        List<CartItem> items = CartServiceUtils.cartItemsOf(cart);
        Optional<CartItem> existingItem = CartServiceUtils.findCartItem(items, variant.id());
        int requestedQuantity = existingItem.map(CartItem::quantity).orElse(0) + request.quantity();
        if (requestedQuantity > cartProperties.maxItemQuantity()) {
            throw new CartItemLimitExceededException(cartProperties.maxItemQuantity());
        }
        CartServiceUtils.requireStockFor(variant, requestedQuantity);
        Instant now = clock.instant();
        BigDecimal unitPrice = CartServiceUtils.currentUnitPrice(product).orElse(null);
        CartItem updatedItem = existingItem
                .map(item -> new CartItem(item.productId(), item.variantId(), requestedQuantity, unitPrice, item.addedAt()))
                .orElseGet(() -> new CartItem(product.id(), variant.id(), requestedQuantity, unitPrice, now));
        List<CartItem> updatedItems = existingItem
                .map(item -> CartServiceUtils.replaceCartItem(items, updatedItem))
                .orElseGet(() -> CartServiceUtils.appendCartItem(items, updatedItem));
        return toCartResponse(saveCart(CartServiceUtils.withItems(cart, updatedItems, now)));
    }

    public CartResponse updateCartItemQuantity(String cartId, String variantId, UpdateCartItemRequest request) {
        Cart cart = requireCart(cartId);
        List<CartItem> items = CartServiceUtils.cartItemsOf(cart);
        CartItem existingItem = CartServiceUtils.findCartItem(items, variantId)
                .orElseThrow(CartItemNotFoundException::new);
        ProductVariant variant = requireAvailableVariant(existingItem.variantId());
        Product product = requireAvailableProduct(variant);
        CartServiceUtils.requireStockFor(variant, request.quantity());
        CartItem updatedItem = new CartItem(existingItem.productId(), existingItem.variantId(), request.quantity(),
                CartServiceUtils.currentUnitPrice(product).orElse(null), existingItem.addedAt());
        List<CartItem> updatedItems = CartServiceUtils.replaceCartItem(items, updatedItem);
        return toCartResponse(saveCart(CartServiceUtils.withItems(cart, updatedItems, clock.instant())));
    }

    public CartResponse removeCartItem(String cartId, String variantId) {
        Cart cart = requireCart(cartId);
        List<CartItem> items = CartServiceUtils.cartItemsOf(cart);
        CartItem existingItem = CartServiceUtils.findCartItem(items, variantId)
                .orElseThrow(CartItemNotFoundException::new);
        List<CartItem> remainingItems = items.stream()
                .filter(item -> !Objects.equals(item.variantId(), existingItem.variantId()))
                .toList();
        return toCartResponse(saveCart(CartServiceUtils.withItems(cart, remainingItems, clock.instant())));
    }

    private Cart requireCart(String cartId) {
        return Optional.ofNullable(cartId)
                .filter(id -> CartConstants.CART_ID_PATTERN.matcher(id).matches())
                .flatMap(cartRepository::findById)
                .orElseThrow(CartNotFoundException::new);
    }

    private ProductVariant requireAvailableVariant(String variantId) {
        return Optional.ofNullable(variantId)
                .filter(ObjectId::isValid)
                .flatMap(productVariantRepository::findById)
                .filter(CartServiceUtils::isActiveVariant)
                .filter(variant -> CartServiceUtils.stockOf(variant) > 0)
                .orElseThrow(VariantUnavailableException::new);
    }

    private Product requireAvailableProduct(ProductVariant variant) {
        return Optional.ofNullable(variant.productId())
                .flatMap(productRepository::findById)
                .filter(Product::active)
                .orElseThrow(VariantUnavailableException::new);
    }

    private Cart saveCart(Cart cart) {
        try {
            return cartRepository.save(cart);
        } catch (OptimisticLockingFailureException exception) {
            log.warn(LogMessages.CART_CONCURRENT_MODIFICATION);
            throw new CartConflictException(exception);
        }
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItem> items = CartServiceUtils.cartItemsOf(cart).stream()
                .sorted(Comparator.comparing(CartItem::addedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<String, Product> productsById = findProductsById(items);
        Map<String, ProductVariant> variantsById = findVariantsById(items);
        List<PricedCartItem> pricedItems = items.stream()
                .map(item -> CartServiceUtils.priceCartItem(
                        item, productsById.get(item.productId()), variantsById.get(item.variantId())))
                .toList();
        int itemCount = items.stream().mapToInt(CartItem::quantity).sum();
        BigDecimal subtotal = pricedItems.stream()
                .filter(pricedItem -> pricedItem.status() != CartItemStatus.UNAVAILABLE)
                .map(PricedCartItem::lineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cartMapper.toCartResponse(
                new PricedCart(cart.id(), pricedItems, itemCount, subtotal, cartProperties.maxItemQuantity()),
                storeProperties);
    }

    private Map<String, Product> findProductsById(List<CartItem> items) {
        List<String> productIds = items.stream().map(CartItem::productId).filter(Objects::nonNull).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::id, Function.identity(), (first, second) -> first));
    }

    private Map<String, ProductVariant> findVariantsById(List<CartItem> items) {
        List<String> variantIds = items.stream().map(CartItem::variantId).filter(Objects::nonNull).distinct().toList();
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        return productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::id, Function.identity(), (first, second) -> first));
    }
}
