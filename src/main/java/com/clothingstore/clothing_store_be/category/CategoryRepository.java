package com.clothingstore.clothing_store_be.category;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByActiveTrue(Sort sort);

    Optional<Category> findBySlugAndActiveTrue(String slug);
}
