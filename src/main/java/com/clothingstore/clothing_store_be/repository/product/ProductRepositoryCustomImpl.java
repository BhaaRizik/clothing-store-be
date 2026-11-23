package com.clothingstore.clothing_store_be.repository.product;

import com.clothingstore.clothing_store_be.model.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Product> findActiveProductsByFilters(Optional<String> categoryId, Optional<Boolean> featured,
            Pageable pageable) {
        Criteria activeProductCriteria = Criteria.where("active").is(true);
        categoryId.ifPresent(id -> activeProductCriteria.and("categoryId").is(id));
        featured.ifPresent(featuredFlag -> activeProductCriteria.and("featured").is(featuredFlag));
        Query filterQuery = Query.query(activeProductCriteria);
        List<Product> products = mongoTemplate.find(Query.of(filterQuery).with(pageable), Product.class);
        return PageableExecutionUtils.getPage(products, pageable,
                () -> mongoTemplate.count(Query.of(filterQuery).limit(-1).skip(-1), Product.class));
    }
}
