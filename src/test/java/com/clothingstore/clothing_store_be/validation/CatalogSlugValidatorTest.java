package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSlugValidatorTest {

    private final CatalogSlugValidator catalogSlugValidator = new CatalogSlugValidator(new CatalogSlugProperties(10));

    @ParameterizedTest
    @ValueSource(strings = {"men", "a", "1", "men-shirt", "t-shirt-2", "a-b-c-d-e", "abcdefghij"})
    void acceptsLowercaseKebabCaseSlugsWithinConfiguredLength(String slug) {
        assertThat(catalogSlugValidator.isValidCatalogSlug(slug)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Men", "MEN", "men_shirt", "men shirt", " men", "men ", "-men", "men-", "men--shirt",
            "men.shirt", "men/shirt", "men%2F", "méne", "abcdefghijk", "men\n"})
    void rejectsSlugsThatAreNotLowercaseKebabCaseOrTooLong(String slug) {
        assertThat(catalogSlugValidator.isValidCatalogSlug(slug)).isFalse();
    }

    @Test
    void usesConfiguredMaximumLength() {
        CatalogSlugValidator longerLimit = new CatalogSlugValidator(new CatalogSlugProperties(11));

        assertThat(longerLimit.isValidCatalogSlug("abcdefghijk")).isTrue();
        assertThat(longerLimit.isValidCatalogSlug("abcdefghijkl")).isFalse();
    }
}
