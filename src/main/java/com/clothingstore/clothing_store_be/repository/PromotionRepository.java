package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.model.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromotionRepository extends MongoRepository<Promotion, String>, PromotionRepositoryCustom {
}
