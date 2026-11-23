package com.clothingstore.clothing_store_be.repository.promotion;

import com.clothingstore.clothing_store_be.model.promotion.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromotionRepository extends MongoRepository<Promotion, String>, PromotionRepositoryCustom {
}
