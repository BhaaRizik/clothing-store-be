package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.model.Promotion;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PromotionRepositoryCustom {

    List<Promotion> findActivePromotionsAt(Instant moment);
}
