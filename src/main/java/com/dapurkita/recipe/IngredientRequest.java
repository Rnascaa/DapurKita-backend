package com.dapurkita.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredientRequest(
        @NotBlank(message = "Nama bahan wajib diisi")
        @Size(max = 100, message = "Nama bahan maksimal 100 karakter")
        String name,

        @NotBlank(message = "Jumlah wajib diisi")
        @Size(max = 50, message = "Jumlah maksimal 50 karakter")
        String quantity,

        @Size(max = 30, message = "Satuan maksimal 30 karakter")
        String unit) {

    public IngredientRequest {
        name = name == null ? null : name.strip();
        quantity = quantity == null ? null : quantity.strip();
        unit = unit == null || unit.isBlank() ? null : unit.strip();
    }
}
