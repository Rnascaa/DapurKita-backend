package com.dapurkita.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nama wajib diisi")
        @Size(min = 2, max = 100, message = "Nama harus 2 sampai 100 karakter")
        String name,

        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 254, message = "Email maksimal 254 karakter")
        String email,

        @NotBlank(message = "Password wajib diisi")
        @Size(min = 8, max = 72, message = "Password harus 8 sampai 72 karakter")
        String password) {

    public RegisterRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip();
    }
}
