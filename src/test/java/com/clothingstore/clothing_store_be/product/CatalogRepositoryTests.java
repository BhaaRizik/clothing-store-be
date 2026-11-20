package com.clothingstore.clothing_store_be.product;

import com.clothingstore.clothing_store_be.TestcontainersConfiguration;
import com.clothingstore.clothing_store_be.category.Category;
import com.clothingstore.clothing_store_be.category.CategoryRepository;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inserts raw BSON shaped exactly like docker/mongo-init/seed.js (ObjectId refs, Decimal128 prices,
 * no {@code _class}) and verifies the Spring Data mapping and active-only queries against a real MongoDB.
 */
@DataMongoTest
@Import(TestcontainersConfiguration.class)
class CatalogRepositoryTests {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private final ObjectId menId = new ObjectId();
    private final ObjectId womenId = new ObjectId();

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection("categories").deleteMany(new Document());
        mongoTemplate.getCollection("products").deleteMany(new Document());

        mongoTemplate.getCollection("categories").insertMany(List.of(
                category(womenId, "women", "Women", 2, true),
                category(menId, "men", "Men", 1, true),
                category(new ObjectId(), "accessories", "Accessories", 1, true),
                category(new ObjectId(), "archive", "Archive", 0, false)));

        mongoTemplate.getCollection("products").insertMany(List.of(
                product("men-shirt", menId, "49.90", "39.90", true, true, 2,
                        List.of(new Document("url", "http://img/1").append("alt", "Front"))),
                product("men-coat", menId, "189.00", null, true, false, 20, List.of()),
                product("men-retired", menId, "10.00", null, false, true, 1, List.of()),
                product("women-dress", womenId, "69.90", null, true, true, 5, List.of())));
    }

    private static Document category(ObjectId id, String slug, String name, int order, boolean active) {
        Date now = new Date();
        return new Document("_id", id).append("slug", slug).append("name", name).append("description", null)
                .append("parentId", null).append("imageUrl", null).append("displayOrder", order)
                .append("active", active).append("createdAt", now).append("updatedAt", now);
    }

    private static Document product(String slug, ObjectId categoryId, String basePrice, String salePrice,
            boolean active, boolean featured, int ageDays, List<Document> images) {
        Date created = Date.from(Instant.now().minusSeconds(ageDays * 86_400L));
        return new Document("_id", new ObjectId()).append("slug", slug).append("name", "Name " + slug)
                .append("description", "Desc").append("categoryId", categoryId).append("brand", null)
                .append("images", images).append("basePrice", Decimal128.parse(basePrice))
                .append("salePrice", salePrice == null ? null : Decimal128.parse(salePrice))
                .append("active", active).append("featured", featured)
                .append("createdAt", created).append("updatedAt", created);
    }

    @Test
    void readsDecimal128AndObjectIdFieldsWithoutClassHint() {
        Product shirt = productRepository.findBySlugAndActiveTrue("men-shirt").orElseThrow();

        assertThat(shirt.basePrice()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(shirt.salePrice()).isEqualByComparingTo(new BigDecimal("39.90"));
        assertThat(shirt.categoryId()).isEqualTo(menId.toHexString());
        assertThat(shirt.images()).containsExactly(new ProductImage("http://img/1", "Front"));
        assertThat(shirt.brand()).isNull();
        assertThat(productRepository.findBySlugAndActiveTrue("men-coat").orElseThrow().salePrice()).isNull();
    }

    @Test
    void inactiveProductIsNotFoundBySlug() {
        assertThat(productRepository.findBySlugAndActiveTrue("men-retired")).isEmpty();
    }

    @Test
    void findActiveFiltersByCategoryObjectIdAndExcludesInactive() {
        Page<Product> page = productRepository.findActive(menId.toHexString(), null,
                PageRequest.of(0, 20, ProductSort.NEWEST.sort()));

        assertThat(page.getContent()).extracting(Product::slug).containsExactly("men-shirt", "men-coat");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findActiveFiltersByFeatured() {
        Page<Product> page = productRepository.findActive(null, true, PageRequest.of(0, 20, ProductSort.NAME.sort()));

        assertThat(page.getContent()).extracting(Product::slug).containsExactly("men-shirt", "women-dress");
    }

    @Test
    void findActiveSortsNumericallyByDecimal128PriceAndPaginates() {
        Page<Product> first = productRepository.findActive(null, null, PageRequest.of(0, 2, ProductSort.PRICE_ASC.sort()));
        Page<Product> second = productRepository.findActive(null, null, PageRequest.of(1, 2, ProductSort.PRICE_ASC.sort()));

        assertThat(first.getContent()).extracting(Product::slug).containsExactly("men-shirt", "women-dress");
        assertThat(second.getContent()).extracting(Product::slug).containsExactly("men-coat");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }

    @Test
    void activeCategoriesSortedByDisplayOrderThenName() {
        List<Category> categories = categoryRepository.findByActiveTrue(
                Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name")));

        assertThat(categories).extracting(Category::slug).containsExactly("accessories", "men", "women");
        assertThat(categoryRepository.findBySlugAndActiveTrue("archive")).isEmpty();
        assertThat(categoryRepository.findBySlugAndActiveTrue("men")).get()
                .extracting(Category::id).isEqualTo(menId.toHexString());
    }
}
