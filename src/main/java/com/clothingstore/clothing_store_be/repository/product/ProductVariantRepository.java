package com.clothingstore.clothing_store_be.repository.product;

import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductVariantRepository extends MongoRepository<ProductVariant, String> {

    List<ProductVariant> findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc(String productId);
}
