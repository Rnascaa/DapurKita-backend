package com.dapurkita.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Body POST dan PUT /recipes. Urutan bahan dan langkah mengikuti urutan array. */
public record RecipeRequest(
        @NotBlank(message = "Judul wajib diisi")
        @Size(min = 3, max = 150, message = "Judul harus 3 sampai 150 karakter")
        String title,

        @Size(max = 2000, message = "Deskripsi maksimal 2000 karakter")
        String description,

        @Size(max = 2048, message = "URL gambar maksimal 2048 karakter")
        @Pattern(regexp = "^(?i)https?://\\S+$", message = "URL gambar harus berawalan http atau https")
        String imageUrl,

        @NotNull(message = "Waktu memasak wajib diisi")
        @Min(value = 1, message = "Waktu memasak harus lebih dari 0")
        Integer cookingTimeMinutes,

        @NotNull(message = "Jumlah porsi wajib diisi")
        @Min(value = 1, message = "Jumlah porsi harus lebih dari 0")
        Integer servings,

        @NotNull(message = "Kategori wajib diisi")
        Long categoryId,

        @NotNull(message = "Bahan wajib diisi")
        @Size(min = 1, max = 50, message = "Bahan harus 1 sampai 50 item")
        @Valid
        List<IngredientRequest> ingredients,

        @NotNull(message = "Langkah wajib diisi")
        @Size(min = 1, max = 50, message = "Langkah harus 1 sampai 50 item")
        @Valid
        List<InstructionRequest> instructions) {

    public RecipeRequest {
        title = title == null ? null : title.strip();
        description = description == null || description.isBlank() ? null : description.strip();
        imageUrl = imageUrl == null ? null : imageUrl.strip();
    }
}
