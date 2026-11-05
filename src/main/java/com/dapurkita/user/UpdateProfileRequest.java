package com.dapurkita.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Nama wajib diisi")
        @Size(min = 2, max = 100, message = "Nama harus 2 sampai 100 karakter")
        String name) {

    public UpdateProfileRequest {
        name = name == null ? null : name.strip();
    }
}
