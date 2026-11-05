package com.dapurkita.auth;

/** Hasil login/register/refresh: body JSON dan nilai mentah refresh token untuk cookie. */
public record AuthResult(AuthResponse response, String refreshToken) {
}
