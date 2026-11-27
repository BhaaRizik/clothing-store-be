package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.constants.CartConstants;
import com.clothingstore.clothing_store_be.constants.LogMessages;
import com.clothingstore.clothing_store_be.model.cart.Cart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexOptions;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CartIndexConfig {

    private final MongoTemplate mongoTemplate;
    private final CartProperties cartProperties;

    @EventListener(ApplicationReadyEvent.class)
    void ensureInactiveCartExpiryIndex() {
        IndexOperations cartIndexOperations = mongoTemplate.indexOps(Cart.class);
        Duration expiry = cartProperties.inactiveCartExpiry();
        Optional<IndexInfo> existingIndex = cartIndexOperations.getIndexInfo().stream()
                .filter(indexInfo -> CartConstants.INACTIVE_CART_EXPIRY_INDEX.equals(indexInfo.getName()))
                .findFirst();
        if (existingIndex.isEmpty()) {
            cartIndexOperations.createIndex(new Index()
                    .on(CartConstants.UPDATED_AT_FIELD, Sort.Direction.ASC)
                    .named(CartConstants.INACTIVE_CART_EXPIRY_INDEX)
                    .expire(expiry));
            log.info(LogMessages.CART_EXPIRY_INDEX_CREATED, expiry);
            return;
        }
        boolean expiryChanged = existingIndex.flatMap(IndexInfo::getExpireAfter)
                .filter(expiry::equals)
                .isEmpty();
        if (expiryChanged) {
            cartIndexOperations.alterIndex(CartConstants.INACTIVE_CART_EXPIRY_INDEX, IndexOptions.expireAfter(expiry));
            log.info(LogMessages.CART_EXPIRY_INDEX_UPDATED, expiry);
        }
    }
}
