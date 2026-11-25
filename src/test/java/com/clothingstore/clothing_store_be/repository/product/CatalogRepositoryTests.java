package com.clothingstore.clothing_store_be.repository.product;

import com.clothingstore.clothing_store_be.TestcontainersConfiguration;
import com.clothingstore.clothing_store_be.model.category.Category;
import com.clothingstore.clothing_store_be.model.product.Product;
import com.clothingstore.clothing_store_be.model.product.ProductImage;
import com.clothingstore.clothing_store_be.model.product.ProductSort;
import com.clothingstore.clothing_store_be.repository.category.CategoryRepository;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
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
        assertThat(shirt.images()).containsExactly(new ProductImage("http://img/1", "Front", null));
        assertThat(shirt.brand()).isNull();
        assertThat(productRepository.findBySlugAndActiveTrue("men-coat").orElseThrow().salePrice()).isNull();
    }

    @Test
    void readsColorTaggedImagesAlongsideLegacyUntaggedImages() {
        mongoTemplate.getCollection("products").insertOne(product("men-tagged", menId, "29.90", null, true, false, 3,
                List.of(new Document("url", "http://img/white").append("alt", "White").append("color", "White"),
                        new Document("url", "http://img/detail").append("alt", "Detail"))));

        assertThat(productRepository.findBySlugAndActiveTrue("men-tagged").orElseThrow().images()).containsExactly(
                new ProductImage("http://img/white", "White", "White"), new ProductImage("http://img/detail", "Detail", null));
    }

    @Test
    void findActiveProductsByFiltersCombinesCategoryAndFeatured() {
        Page<Product> page = productRepository.findActiveProductsByFilters(Optional.of(menId.toHexString()),
                Optional.of(false), PageRequest.of(0, 20, ProductSort.NEWEST.getSort()));

        assertThat(page.getContent()).extracting(Product::slug).containsExactly("men-coat");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void inactiveProductIsNotFoundBySlug() {
        assertThat(productRepository.findBySlugAndActiveTrue("men-retired")).isEmpty();
    }

    @Test
    void findActiveProductsByFiltersFiltersByCategoryObjectIdAndExcludesInactive() {
        Page<Product> page = productRepository.findActiveProductsByFilters(Optional.of(menId.toHexString()), Optional.empty(),
                PageRequest.of(0, 20, ProductSort.NEWEST.getSort()));

        assertThat(page.getContent()).extracting(Product::slug).containsExactly("men-shirt", "men-coat");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findActiveProductsByFiltersFiltersByFeatured() {
        Page<Product> page = productRepository.findActiveProductsByFilters(Optional.empty(), Optional.of(true), PageRequest.of(0, 20, ProductSort.NAME.getSort()));

        assertThat(page.getContent()).extracting(Product::slug).containsExactly("men-shirt", "women-dress");
    }

    @Test
    void findActiveProductsByFiltersSortsNumericallyByDecimal128PriceAndPaginates() {
        Page<Product> first = productRepository.findActiveProductsByFilters(Optional.empty(), Optional.empty(), PageRequest.of(0, 2, ProductSort.PRICE_ASC.getSort()));
        Page<Product> second = productRepository.findActiveProductsByFilters(Optional.empty(), Optional.empty(), PageRequest.of(1, 2, ProductSort.PRICE_ASC.getSort()));

        assertThat(first.getContent()).extracting(Product::slug).containsExactly("men-shirt", "women-dress");
        assertThat(second.getContent()).extracting(Product::slug).containsExactly("men-coat");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }

    @Test
    void activeCategoriesSortedByDisplayOrderThenName() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();

        assertThat(categories).extracting(Category::slug).containsExactly("accessories", "men", "women");
        assertThat(categoryRepository.findBySlugAndActiveTrue("archive")).isEmpty();
        assertThat(categoryRepository.findBySlugAndActiveTrue("men")).get()
                .extracting(Category::id).isEqualTo(menId.toHexString());
    }
}
