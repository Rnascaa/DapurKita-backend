package com.dapurkita.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Menjaga data contoh tetap valid terhadap aturan kontrak, tanpa database. */
class SampleRecipesTest {

    @Test
    void sampleRecipesSatisfyContractValidationAndConstraints() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        List<RecipeRequest> samples = SampleRecipeSeeder.samples();
        assertTrue(samples.size() >= 15);

        Set<String> titles = new HashSet<>();
        Set<Long> categories = new HashSet<>();
        for (RecipeRequest sample : samples) {
            assertTrue(validator.validate(sample).isEmpty(), "Tidak valid: " + sample.title());
            assertTrue(titles.add(sample.title()), "Judul ganda: " + sample.title());
            assertTrue(sample.categoryId() >= 1 && sample.categoryId() <= 8, sample.title());
            categories.add(sample.categoryId());
        }
        assertEquals(8, categories.size(), "Semua kategori harus punya contoh");
    }
}
