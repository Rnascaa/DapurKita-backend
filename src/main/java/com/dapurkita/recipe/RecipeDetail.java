package com.dapurkita.recipe;

import com.dapurkita.category.CategoryRef;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record RecipeDetail(
        Long id,
        String title,
        String description,
        String imageUrl,
        int cookingTimeMinutes,
        int servings,
        CategoryRef category,
        @JsonProperty("isFavorited") boolean isFavorited,
        Instant createdAt,
        List<IngredientResponse> ingredients,
        List<InstructionResponse> instructions,
        Instant updatedAt) {

    public record IngredientResponse(String name, String quantity, String unit) {
    }

    public record InstructionResponse(int stepNumber, String description) {
    }
}
