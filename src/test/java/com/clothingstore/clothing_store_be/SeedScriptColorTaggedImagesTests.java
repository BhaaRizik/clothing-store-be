package com.clothingstore.clothing_store_be;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SeedScriptColorTaggedImagesTests {

    private static final String SEED_IN_CONTAINER = "/qa/seed.js";
    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");
    private static final Set<String> COLOR_TAGGED_SLUGS = Set.of("men-classic-oxford-shirt", "men-slim-chino-trousers",
            "women-cable-knit-sweater", "women-ribbed-tank-top", "shoes-running-trainers", "acc-wool-beanie",
            "acc-classic-sunglasses");

    private static MongoDBContainer mongo;
    private static MongoClient client;

    @BeforeAll
    static void startMongo() {
        mongo = new MongoDBContainer(DockerImageName.parse("mongo:8.0.38"))
                .withCopyFileToContainer(MountableFile.forHostPath("docker/mongo-init/seed.js"), SEED_IN_CONTAINER);
        mongo.start();
        client = MongoClients.create(mongo.getConnectionString());
    }

    @AfterAll
    static void stopMongo() {
        client.close();
        mongo.stop();
    }

    @BeforeEach
    void dropDatabase() {
        database().drop();
    }

    private static MongoDatabase database() {
        return client.getDatabase("clothing_store");
    }

    private static String runSeed() throws Exception {
        Container.ExecResult result = mongo.execInContainer("mongosh", "--quiet", SEED_IN_CONTAINER);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        return result.getStdout();
    }

    private static List<Document> images(Document product) {
        return product.getList("images", Document.class);
    }

    private static Document product(String slug) {
        return database().getCollection("products").find(new Document("slug", slug)).first();
    }

    private static Set<String> variantColorNames(ObjectId productId) {
        return database().getCollection("product_variants")
                .distinct("color.name", new Document("productId", productId), String.class)
                .into(new HashSet<>());
    }

    @Test
    void freshSeedTagsImagesOfExactlyTheSevenMultiColorProductsWithTheirVariantColors() throws Exception {
        runSeed();

        List<String> taggedSlugs = new ArrayList<>();
        for (Document product : database().getCollection("products").find()) {
            List<Document> productImages = images(product);
            if (productImages.stream().noneMatch(image -> image.containsKey("color"))) {
                continue;
            }
            taggedSlugs.add(product.getString("slug"));
            Set<String> variantColors = variantColorNames(product.getObjectId("_id"));
            assertThat(productImages).as(product.getString("slug")).allSatisfy(image -> {
                assertThat(variantColors).contains(image.getString("color"));
                assertThat(image.getString("alt")).containsIgnoringCase(image.getString("color"));
            });
            assertThat(productImages.stream().map(image -> image.getString("color")).collect(Collectors.toSet()))
                    .as("every variant color of %s has at least one image", product.getString("slug"))
                    .isEqualTo(variantColors);
        }
        assertThat(taggedSlugs).containsExactlyInAnyOrderElementsOf(COLOR_TAGGED_SLUGS);
    }

    @Test
    void everyImageUrlWrittenBySeedPointsToAnExistingStaticFile() throws Exception {
        runSeed();

        List<String> urls = new ArrayList<>();
        database().getCollection("products").find()
                .forEach(product -> images(product).forEach(image -> urls.add(image.getString("url"))));
        assertThat(urls).hasSize(25).doesNotHaveDuplicates();
        assertThat(urls).allSatisfy(url -> assertThat(Files.isRegularFile(STATIC_ROOT.resolve(url.substring(1))))
                .as(url).isTrue());
    }

    @Test
    void reRunningSeedOnAnAlreadySeededDatabaseChangesNoProductImages() throws Exception {
        runSeed();
        Map<String, Object> before = imagesAndUpdatedAtBySlug();

        String output = runSeed();

        assertThat(output).contains("color-tagged product images: 0 products updated, 7 products have color-tagged images.");
        assertThat(imagesAndUpdatedAtBySlug()).isEqualTo(before);
    }

    @Test
    void seedUpgradesAnExistingDatabaseWithUntaggedImagesAndLeavesOtherProductsAlone() throws Exception {
        Date legacyUpdatedAt = new Date(1_700_000_000_000L);
        ObjectId category = new ObjectId();
        database().getCollection("categories").insertOne(new Document("_id", category).append("slug", "men")
                .append("name", "Men").append("active", true));
        database().getCollection("products").insertMany(List.of(
                legacyProduct(category, "men-classic-oxford-shirt", "/images/catalog/men-classic-oxford-shirt.svg",
                        legacyUpdatedAt),
                legacyProduct(category, "men-wool-blend-overcoat", "/images/catalog/men-wool-blend-overcoat.svg",
                        legacyUpdatedAt)));

        String output = runSeed();

        assertThat(output).contains("skipping catalog seed")
                .contains("color-tagged product images: 1 products updated, 1 products have color-tagged images.");
        Document shirt = product("men-classic-oxford-shirt");
        assertThat(images(shirt)).extracting(image -> image.getString("url"), image -> image.getString("color"))
                .containsExactly(
                        tuple("/images/catalog/men-classic-oxford-shirt-light-blue.svg",
                                "Light blue"),
                        tuple("/images/catalog/men-classic-oxford-shirt-white.svg",
                                "White"));
        assertThat(shirt.getDate("updatedAt")).isAfter(legacyUpdatedAt);
        Document coat = product("men-wool-blend-overcoat");
        assertThat(images(coat)).extracting(image -> image.getString("url"))
                .containsExactly("/images/catalog/men-wool-blend-overcoat.svg");
        assertThat(coat.getDate("updatedAt")).isEqualTo(legacyUpdatedAt);
    }

    @Test
    void seedNeverOverwritesImagesThatAlreadyCarryAColorTag() throws Exception {
        Date curatedUpdatedAt = new Date(1_700_000_000_000L);
        ObjectId category = new ObjectId();
        database().getCollection("categories").insertOne(new Document("_id", category).append("slug", "women")
                .append("name", "Women").append("active", true));
        Document curated = legacyProduct(category, "women-ribbed-tank-top", "/images/catalog/curated.svg",
                curatedUpdatedAt);
        images(curated).getFirst().append("color", "Black");
        database().getCollection("products").insertOne(curated);

        runSeed();

        Document tankTop = product("women-ribbed-tank-top");
        assertThat(images(tankTop)).extracting(image -> image.getString("url"), image -> image.getString("color"))
                .containsExactly(tuple("/images/catalog/curated.svg", "Black"));
        assertThat(tankTop.getDate("updatedAt")).isEqualTo(curatedUpdatedAt);
    }

    private static Document legacyProduct(ObjectId category, String slug, String url, Date updatedAt) {
        List<Document> legacyImages = new ArrayList<>();
        legacyImages.add(new Document("url", url).append("alt", "Legacy image"));
        return new Document("_id", new ObjectId()).append("slug", slug).append("name", slug)
                .append("categoryId", category).append("images", legacyImages).append("active", true)
                .append("updatedAt", updatedAt);
    }

    private static Map<String, Object> imagesAndUpdatedAtBySlug() {
        Map<String, Object> snapshot = new HashMap<>();
        database().getCollection("products").find().forEach(product -> snapshot.put(product.getString("slug"),
                List.of(product.get("images"), product.get("updatedAt"))));
        return snapshot;
    }
}
