package com.dapurkita.auth;

import com.dapurkita.common.exception.ApiException;
import com.dapurkita.config.AppProperties;
import com.dapurkita.security.JwtService;
import com.dapurkita.user.Role;
import com.dapurkita.user.User;
import com.dapurkita.user.UserRepository;
import com.dapurkita.user.UserResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwt, AppProperties props) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
        // Dipakai agar waktu respons login tidak membocorkan apakah email terdaftar.
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.validation("password", "Password maksimal 72 byte");
        }
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.emailExists();
        }
        User user = new User(request.name(), email, encoder.encode(request.password()), Role.USER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.emailExists();
        }
        return issueSession(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email())).orElse(null);
        boolean matches = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) {
            throw ApiException.invalidCredentials();
        }
        return issueSession(user);
    }

    /**
     * Rotasi refresh token. Token lama dicabut. Jika token yang sudah dicabut dipakai lagi
     * (indikasi pencurian), seluruh sesi user dicabut. noRollbackFor agar pencabutan tetap tersimpan.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.unauthorized("Sesi tidak valid");
        }
        RefreshToken token = refreshTokens.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("Sesi tidak valid"));
        Instant now = Instant.now();
        if (token.getRevokedAt() != null) {
            refreshTokens.revokeAllForUser(token.getUser().getId(), now);
            throw ApiException.unauthorized("Sesi tidak valid");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized("Sesi berakhir");
        }
        token.setRevokedAt(now);
        return issueSession(token.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    private AuthResult issueSession(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(user, hash(raw), Instant.now().plus(props.refresh().ttl())));
        AuthResponse body = new AuthResponse(jwt.issueAccessToken(user), "Bearer",
                jwt.expiresInSeconds(), UserResponse.from(user));
        return new AuthResult(body, raw);
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
