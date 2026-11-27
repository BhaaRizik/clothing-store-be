package com.clothingstore.clothing_store_be.repository.cart;

import com.clothingstore.clothing_store_be.model.cart.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {
}
