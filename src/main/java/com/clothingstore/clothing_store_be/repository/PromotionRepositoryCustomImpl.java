package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.model.Promotion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PromotionRepositoryCustomImpl implements PromotionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Promotion> findActivePromotionsAt(Instant moment) {
        Criteria startedCriteria = new Criteria().orOperator(
                Criteria.where("startsAt").is(null),
                Criteria.where("startsAt").lte(moment));
        Criteria notEndedCriteria = new Criteria().orOperator(
                Criteria.where("endsAt").is(null),
                Criteria.where("endsAt").gt(moment));
        Criteria activePromotionCriteria = Criteria.where("active").is(true)
                .andOperator(startedCriteria, notEndedCriteria);
        Query activePromotionQuery = Query.query(activePromotionCriteria)
                .with(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("title")));
        return mongoTemplate.find(activePromotionQuery, Promotion.class);
    }
}
