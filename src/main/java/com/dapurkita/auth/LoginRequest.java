package com.dapurkita.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email wajib diisi") String email,
        @NotBlank(message = "Password wajib diisi") String password) {

    public LoginRequest {
        email = email == null ? null : email.strip();
    }
}
