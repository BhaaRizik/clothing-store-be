package com.clothingstore.clothing_store_be.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface ProductRepositoryCustom {

    /**
     * Pages through active products, optionally restricted to one category and/or the featured flag.
     *
     * @param categoryId category id, or {@code null} for all categories
     * @param featured   featured flag, or {@code null} for no filter
     */
    Page<Product> findActive(String categoryId, Boolean featured, Pageable pageable);
}
