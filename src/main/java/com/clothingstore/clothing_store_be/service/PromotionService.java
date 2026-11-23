package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.PromotionResponse;
import com.clothingstore.clothing_store_be.mapper.PromotionMapper;
import com.clothingstore.clothing_store_be.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final Clock clock;

    public List<PromotionResponse> listActivePromotions() {
        return promotionMapper.toPromotionResponses(promotionRepository.findActivePromotionsAt(Instant.now(clock)));
    }
}
