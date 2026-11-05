package com.dapurkita.recipe;

import com.dapurkita.category.CategoryRef;

public final class RecipeMapper {

    private RecipeMapper() {
    }

    public static RecipeSummary toSummary(Recipe r, boolean favorited) {
        return new RecipeSummary(r.getId(), r.getTitle(), r.getDescription(), r.getImageUrl(),
                r.getCookingTimeMinutes(), r.getServings(), CategoryRef.from(r.getCategory()),
                favorited, r.getCreatedAt());
    }

    /** Harus dipanggil di dalam transaksi karena membaca koleksi lazy. */
    public static RecipeDetail toDetail(Recipe r, boolean favorited) {
        return new RecipeDetail(r.getId(), r.getTitle(), r.getDescription(), r.getImageUrl(),
                r.getCookingTimeMinutes(), r.getServings(), CategoryRef.from(r.getCategory()),
                favorited, r.getCreatedAt(),
                r.getIngredients().stream()
                        .map(i -> new RecipeDetail.IngredientResponse(i.getName(), i.getQuantity(), i.getUnit()))
                        .toList(),
                r.getInstructions().stream()
                        .map(s -> new RecipeDetail.InstructionResponse(s.getStepNumber(), s.getDescription()))
                        .toList(),
                r.getUpdatedAt());
    }
}
