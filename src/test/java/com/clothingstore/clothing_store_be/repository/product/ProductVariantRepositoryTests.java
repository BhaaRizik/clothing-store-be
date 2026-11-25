package com.clothingstore.clothing_store_be.repository.product;

import com.clothingstore.clothing_store_be.TestcontainersConfiguration;
import com.clothingstore.clothing_store_be.model.product.ProductColor;
import com.clothingstore.clothing_store_be.model.product.ProductVariant;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProductVariantRepositoryTests {

    private static final String COLLECTION = "product_variants";

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ProductVariantRepository productVariantRepository;

    private final ObjectId shirtId = new ObjectId();
    private final ObjectId dressId = new ObjectId();
    private final ObjectId shirtLargeId = new ObjectId("650000000000000000000c01");
    private final ObjectId shirtLargeTwinId = new ObjectId("650000000000000000000c02");

    @BeforeEach
    void seed() {
        mongoTemplate.getCollection(COLLECTION).deleteMany(new Document());
        mongoTemplate.getCollection(COLLECTION).insertMany(List.of(
                variant(new ObjectId(), shirtId, "SHIRT-BLU-XL", "XL", color("Blue", "#1E3A8A"), 4, true, 30),
                variant(shirtLargeTwinId, shirtId, "SHIRT-WHT-L", "L", color("White", "#FFFFFF"), 0, true, 21),
                variant(new ObjectId(), shirtId, "SHIRT-BLU-S", "S", color("Blue", "#1E3A8A"), 9, true, 0),
                variant(shirtLargeId, shirtId, "SHIRT-BLU-L", "L", color("Blue", "#1E3A8A"), 2, true, 21),
                variant(new ObjectId(), shirtId, "SHIRT-BLU-M", "M", color("Blue", "#1E3A8A"), 5, false, 10),
                variant(new ObjectId(), dressId, "DRESS-FLO-M", "M", color("Floral print", null), 1, true, 10)));
    }

    private static Document color(String name, String hexCode) {
        return new Document("name", name).append("hexCode", hexCode);
    }

    private static Document variant(ObjectId id, ObjectId productId, String sku, String size, Document color,
            int stockQuantity, boolean active, int displayOrder) {
        Date created = Date.from(Instant.parse("2026-01-10T00:00:00Z"));
        return new Document("_id", id).append("productId", productId).append("sku", sku).append("size", size)
                .append("color", color).append("stockQuantity", stockQuantity).append("active", active)
                .append("displayOrder", displayOrder).append("createdAt", created).append("updatedAt", created);
    }

    @Test
    void findsOnlyActiveVariantsOfTheProductByObjectIdOrderedByDisplayOrderThenId() {
        List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc(shirtId.toHexString());

        assertThat(variants).extracting(ProductVariant::sku)
                .containsExactly("SHIRT-BLU-S", "SHIRT-BLU-L", "SHIRT-WHT-L", "SHIRT-BLU-XL");
        assertThat(variants).extracting(ProductVariant::productId).containsOnly(shirtId.toHexString());
        assertThat(variants.get(1).id()).isEqualTo(shirtLargeId.toHexString());
        assertThat(variants.get(2).id()).isEqualTo(shirtLargeTwinId.toHexString());
    }

    @Test
    void readsEmbeddedColorWithNullHexCodeAndStock() {
        List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc(dressId.toHexString());

        assertThat(variants).singleElement().satisfies(variant -> {
            assertThat(variant.color()).isEqualTo(new ProductColor("Floral print", null));
            assertThat(variant.size()).isEqualTo("M");
            assertThat(variant.stockQuantity()).isEqualTo(1);
        });
    }

    @Test
    void productWithoutVariantsReturnsEmptyList() {
        assertThat(productVariantRepository.findByProductIdAndActiveTrueOrderByDisplayOrderAscIdAsc(
                new ObjectId().toHexString())).isEmpty();
    }

    @Test
    void savedVariantStoresProductIdAsObjectId() {
        ObjectId productId = new ObjectId();
        productVariantRepository.save(new ProductVariant(null, productId.toHexString(), "NEW-SKU-1", null, null, 3,
                true, 0, Instant.EPOCH, Instant.EPOCH));

        Document stored = mongoTemplate.getCollection(COLLECTION).find(new Document("sku", "NEW-SKU-1")).first();

        assertThat(stored).isNotNull();
        assertThat(stored.get("productId")).isEqualTo(productId);
    }

    @Test
    void declaresUniqueSkuAndProductActiveOrderIndexes() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(ProductVariant.class).getIndexInfo();

        assertThat(indexes).extracting(IndexInfo::getName)
                .contains("uk_product_variants_sku", "ix_product_variants_product_active_order");
        assertThat(indexes).filteredOn(index -> index.getName().equals("uk_product_variants_sku"))
                .singleElement().extracting(IndexInfo::isUnique).isEqualTo(true);
        assertThatThrownBy(() -> productVariantRepository.save(new ProductVariant(null, shirtId.toHexString(),
                "SHIRT-BLU-S", "S", null, 1, true, 0, Instant.EPOCH, Instant.EPOCH)))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
