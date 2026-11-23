package com.clothingstore.clothing_store_be.repository.product;

import com.clothingstore.clothing_store_be.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepositoryCustom {

    Page<Product> findActiveProductsByFilters(Optional<String> categoryId, Optional<Boolean> featured, Pageable pageable);
}
