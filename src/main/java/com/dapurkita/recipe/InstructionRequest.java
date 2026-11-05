package com.dapurkita.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstructionRequest(
        @NotBlank(message = "Langkah wajib diisi")
        @Size(max = 2000, message = "Langkah maksimal 2000 karakter")
        String description) {

    public InstructionRequest {
        description = description == null ? null : description.strip();
    }
}
