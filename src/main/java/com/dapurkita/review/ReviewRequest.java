package com.dapurkita.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "Rating wajib diisi")
        @Min(value = 1, message = "Rating harus 1 sampai 5")
        @Max(value = 5, message = "Rating harus 1 sampai 5")
        Integer rating,

        @Size(max = 500, message = "Ulasan maksimal 500 karakter")
        String content) {

    /** Teks kosong disimpan sebagai null. */
    public ReviewRequest {
        content = content == null || content.isBlank() ? null : content.strip();
    }
}
