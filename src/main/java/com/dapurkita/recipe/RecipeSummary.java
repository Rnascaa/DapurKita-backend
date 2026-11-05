package com.dapurkita.recipe;

import com.dapurkita.category.CategoryRef;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Item di daftar resep. Tidak memuat penulis (field author dihapus di v2). */
public record RecipeSummary(
        Long id,
        String title,
        String description,
        String imageUrl,
        int cookingTimeMinutes,
        int servings,
        CategoryRef category,
        @JsonProperty("isFavorited") boolean isFavorited,
        Instant createdAt) {
}
