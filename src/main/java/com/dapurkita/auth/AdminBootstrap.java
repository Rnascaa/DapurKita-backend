package com.dapurkita.auth;

import com.dapurkita.config.AppProperties;
import com.dapurkita.user.Role;
import com.dapurkita.user.User;
import com.dapurkita.user.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Membuat akun ADMIN pertama saat start jika belum ada. Dev: nilai dari application-dev.yml.
 * Production: dari environment ADMIN_EMAIL dan ADMIN_PASSWORD. Admin tidak bisa dibuat lewat API.
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        String password = props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 8) {
            log.error("ADMIN_PASSWORD terlalu pendek (minimal 8 karakter), admin tidak dibuat");
            return;
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) {
            return;
        }
        users.save(new User("Admin DapurKita", normalized, encoder.encode(password), Role.ADMIN));
        log.info("Akun admin dibuat: {}", normalized);
    }
}
