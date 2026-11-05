package com.dapurkita.auth;

import com.dapurkita.user.UserResponse;

/** Refresh token TIDAK ada di body, dikirim lewat cookie HttpOnly. */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
