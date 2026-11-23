package com.clothingstore.clothing_store_be.repository.promotion;

import com.clothingstore.clothing_store_be.model.promotion.Promotion;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PromotionRepositoryCustom {

    List<Promotion> findActivePromotionsAt(Instant moment);
}
