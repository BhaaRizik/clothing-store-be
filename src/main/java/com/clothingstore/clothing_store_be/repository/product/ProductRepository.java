package com.clothingstore.clothing_store_be.repository.product;

import com.clothingstore.clothing_store_be.model.product.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {

    Optional<Product> findBySlugAndActiveTrue(String slug);
}
