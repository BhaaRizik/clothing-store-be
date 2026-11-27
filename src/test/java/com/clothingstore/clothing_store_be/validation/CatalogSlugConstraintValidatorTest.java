package com.clothingstore.clothing_store_be.validation;

import com.clothingstore.clothing_store_be.config.CatalogSlugProperties;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogSlugConstraintValidatorTest {

    private final CatalogSlugConstraintValidator catalogSlugConstraintValidator =
            new CatalogSlugConstraintValidator(new CatalogSlugProperties(10));

    private ConstraintValidatorContext context;
    private ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void createContext() {
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Test
    void nullIsValidBecauseTheCategoryFilterIsOptional() {
        assertThat(catalogSlugConstraintValidator.isValid(null, context)).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"men", "a", "1", "men-shirt", "t-shirt-2", "a-b-c-d-e", "abcdefghij"})
    void acceptsLowercaseKebabCaseSlugsWithinConfiguredLength(String slug) {
        assertThat(catalogSlugConstraintValidator.isValid(slug, context)).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"Men", "MEN", "men_shirt", "men shirt", " men", "men ", "-men", "men-", "men--shirt",
            "men.shirt", "men/shirt", "men%2F", "méne", "abcdefghijk", "men\n", "{$ne:1}"})
    void rejectsSlugsThatAreNotLowercaseKebabCaseOrTooLongWithConfiguredMaximumInMessage(String slug) {
        assertThat(catalogSlugConstraintValidator.isValid(slug, context)).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "must be a lowercase kebab-case slug of at most 10 characters");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void usesConfiguredMaximumLength() {
        CatalogSlugConstraintValidator longerLimit = new CatalogSlugConstraintValidator(new CatalogSlugProperties(11));
        ConstraintValidatorContext unusedContext = mock(ConstraintValidatorContext.class);

        assertThat(longerLimit.isValid("abcdefghijk", unusedContext)).isTrue();
        verifyNoInteractions(unusedContext);
        assertThat(longerLimit.isValid("abcdefghijkl", context)).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "must be a lowercase kebab-case slug of at most 11 characters");
    }
}
