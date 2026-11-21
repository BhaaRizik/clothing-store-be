package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<Category> findBySlugAndActiveTrue(String slug);
}
