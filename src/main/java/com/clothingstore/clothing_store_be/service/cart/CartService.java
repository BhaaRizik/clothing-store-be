package com.clothingstore.clothing_store_be.service.cart;

import com.clothingstore.clothing_store_be.config.CartProperties;
import com.clothingstore.clothing_store_be.config.StoreProperties;
import com.clothingstore.clothing_store_be.constants.CartConstants;
import com.clothingstore.clothing_store_be.constants.LogMessages;
import com.clothingstore.clothing_store_be.constants.MoneyConstants;
import com.clothingstore.clothing_store_be.dto.cart.AddCartItemRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartResponse;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.exception.cart.CartConflictException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemLimitExceededException;
import com.clothingstore.clothing_store_be.exception.cart.CartItemNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.CartNotFoundException;
import com.clothingstore.clothing_store_be.exception.cart.InsufficientStockException;
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
import java.util.stream.Stream;

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
        List<CartItem> items = cartItemsOf(cart);
        Optional<CartItem> existingItem = findCartItem(items, variant.id());
        int requestedQuantity = existingItem.map(CartItem::quantity).orElse(0) + request.quantity();
        if (requestedQuantity > cartProperties.maxItemQuantity()) {
            throw new CartItemLimitExceededException(cartProperties.maxItemQuantity());
        }
        requireStockFor(variant, requestedQuantity);
        Instant now = clock.instant();
        BigDecimal unitPrice = currentUnitPrice(product).orElse(null);
        CartItem updatedItem = existingItem
                .map(item -> new CartItem(item.productId(), item.variantId(), requestedQuantity, unitPrice, item.addedAt()))
                .orElseGet(() -> new CartItem(product.id(), variant.id(), requestedQuantity, unitPrice, now));
        List<CartItem> updatedItems = existingItem
                .map(item -> replaceCartItem(items, updatedItem))
                .orElseGet(() -> appendCartItem(items, updatedItem));
        return toCartResponse(saveCart(withItems(cart, updatedItems, now)));
    }

    public CartResponse updateCartItemQuantity(String cartId, String variantId, UpdateCartItemRequest request) {
        Cart cart = requireCart(cartId);
        List<CartItem> items = cartItemsOf(cart);
        CartItem existingItem = findCartItem(items, variantId).orElseThrow(CartItemNotFoundException::new);
        ProductVariant variant = requireAvailableVariant(existingItem.variantId());
        Product product = requireAvailableProduct(variant);
        requireStockFor(variant, request.quantity());
        CartItem updatedItem = new CartItem(existingItem.productId(), existingItem.variantId(), request.quantity(),
                currentUnitPrice(product).orElse(null), existingItem.addedAt());
        return toCartResponse(saveCart(withItems(cart, replaceCartItem(items, updatedItem), clock.instant())));
    }

    public CartResponse removeCartItem(String cartId, String variantId) {
        Cart cart = requireCart(cartId);
        List<CartItem> items = cartItemsOf(cart);
        CartItem existingItem = findCartItem(items, variantId).orElseThrow(CartItemNotFoundException::new);
        List<CartItem> remainingItems = items.stream()
                .filter(item -> !Objects.equals(item.variantId(), existingItem.variantId()))
                .toList();
        return toCartResponse(saveCart(withItems(cart, remainingItems, clock.instant())));
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
                .filter(CartService::isActiveVariant)
                .filter(variant -> stockOf(variant) > 0)
                .orElseThrow(VariantUnavailableException::new);
    }

    private Product requireAvailableProduct(ProductVariant variant) {
        return Optional.ofNullable(variant.productId())
                .flatMap(productRepository::findById)
                .filter(Product::active)
                .orElseThrow(VariantUnavailableException::new);
    }

    private static void requireStockFor(ProductVariant variant, int quantity) {
        if (quantity > stockOf(variant)) {
            throw new InsufficientStockException();
        }
    }

    private Cart saveCart(Cart cart) {
        try {
            return cartRepository.save(cart);
        } catch (OptimisticLockingFailureException exception) {
            log.warn(LogMessages.CART_CONCURRENT_MODIFICATION);
            throw new CartConflictException(exception);
        }
    }

    private static Cart withItems(Cart cart, List<CartItem> items, Instant updatedAt) {
        return new Cart(cart.id(), items, cart.createdAt(), updatedAt, cart.version());
    }

    private static List<CartItem> cartItemsOf(Cart cart) {
        return Optional.ofNullable(cart.items()).orElseGet(List::of);
    }

    private static Optional<CartItem> findCartItem(List<CartItem> items, String variantId) {
        return items.stream().filter(item -> Objects.equals(item.variantId(), variantId)).findFirst();
    }

    private static List<CartItem> replaceCartItem(List<CartItem> items, CartItem updatedItem) {
        return items.stream()
                .map(item -> Objects.equals(item.variantId(), updatedItem.variantId()) ? updatedItem : item)
                .toList();
    }

    private static List<CartItem> appendCartItem(List<CartItem> items, CartItem newItem) {
        return Stream.concat(items.stream(), Stream.of(newItem)).toList();
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItem> items = cartItemsOf(cart).stream()
                .sorted(Comparator.comparing(CartItem::addedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<String, Product> productsById = findProductsById(items);
        Map<String, ProductVariant> variantsById = findVariantsById(items);
        List<PricedCartItem> pricedItems = items.stream()
                .map(item -> priceCartItem(item, productsById.get(item.productId()), variantsById.get(item.variantId())))
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

    private static PricedCartItem priceCartItem(CartItem item, Product product, ProductVariant variant) {
        Optional<Product> currentProduct = Optional.ofNullable(product);
        Optional<BigDecimal> unitPrice = currentProduct.flatMap(CartService::currentUnitPrice).map(CartService::toMoneyScale);
        BigDecimal originalPrice = currentProduct
                .filter(availableProduct -> availableProduct.salePrice() != null)
                .map(Product::basePrice)
                .orElse(null);
        BigDecimal lineTotal = unitPrice.map(price -> price.multiply(BigDecimal.valueOf(item.quantity()))).orElse(null);
        boolean priceChanged = unitPrice
                .flatMap(price -> Optional.ofNullable(item.unitPriceSnapshot())
                        .map(snapshot -> toMoneyScale(snapshot).compareTo(price) != 0))
                .orElse(false);
        return new PricedCartItem(item, product, variant, unitPrice.orElse(null), originalPrice, lineTotal,
                statusOf(item, product, variant), priceChanged);
    }

    private static CartItemStatus statusOf(CartItem item, Product product, ProductVariant variant) {
        boolean productAvailable = Optional.ofNullable(product).filter(Product::active).isPresent();
        Optional<ProductVariant> activeVariant = Optional.ofNullable(variant).filter(CartService::isActiveVariant);
        int stock = activeVariant.map(CartService::stockOf).orElse(0);
        if (!productAvailable || stock <= 0) {
            return CartItemStatus.UNAVAILABLE;
        }
        if (stock < item.quantity()) {
            return CartItemStatus.INSUFFICIENT_STOCK;
        }
        return CartItemStatus.AVAILABLE;
    }

    private static Optional<BigDecimal> currentUnitPrice(Product product) {
        return Optional.ofNullable(product.salePrice()).or(() -> Optional.ofNullable(product.basePrice()));
    }

    private static BigDecimal toMoneyScale(BigDecimal amount) {
        return amount.setScale(MoneyConstants.AMOUNT_FRACTION_DIGITS, MoneyConstants.AMOUNT_ROUNDING_MODE);
    }

    private static boolean isActiveVariant(ProductVariant variant) {
        return Boolean.TRUE.equals(variant.active());
    }

    private static int stockOf(ProductVariant variant) {
        return Optional.ofNullable(variant.stockQuantity()).orElse(0);
    }
}
