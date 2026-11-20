package com.clothingstore.clothing_store_be.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

/** Dynamic product filter built with {@link MongoTemplate} (mapping converts categoryId to ObjectId). */
class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    ProductRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Product> findActive(String categoryId, Boolean featured, Pageable pageable) {
        Criteria criteria = Criteria.where("active").is(true);
        if (categoryId != null) {
            criteria = criteria.and("categoryId").is(categoryId);
        }
        if (featured != null) {
            criteria = criteria.and("featured").is(featured);
        }
        Query query = Query.query(criteria);
        List<Product> content = mongoTemplate.find(Query.of(query).with(pageable), Product.class);
        return PageableExecutionUtils.getPage(content, pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class));
    }
}
