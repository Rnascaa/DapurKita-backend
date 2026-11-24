package com.dapurkita.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** @return id user dari klaim "sub", atau null jika request anonim */
    public static Long idOrNull(Jwt jwt) {
        return jwt == null ? null : Long.valueOf(jwt.getSubject());
    }

    /** Peran dibaca dari klaim "role" token yang sudah diverifikasi. */
    public static boolean isAdmin(Jwt jwt) {
        return jwt != null && "ADMIN".equals(jwt.getClaimAsString("role"));
    }
}
