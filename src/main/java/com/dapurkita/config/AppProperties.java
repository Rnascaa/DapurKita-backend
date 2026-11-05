package com.dapurkita.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Refresh refresh,
        Cookie cookie,
        @DefaultValue("12") int bcryptStrength,
        @DefaultValue RateLimit rateLimit,
        @DefaultValue Admin admin) {

    public record Jwt(String secret, @DefaultValue("dapurkita") String issuer,
                      @DefaultValue("15m") Duration accessTtl) {
    }

    public record Refresh(@DefaultValue("7d") Duration ttl) {
    }

    public record Cookie(@DefaultValue("true") boolean secure) {
    }

    public record RateLimit(@DefaultValue("true") boolean enabled,
                            @DefaultValue("10") int loginPerMinute,
                            @DefaultValue("5") int registerPerMinute) {
    }

    public record Admin(String email, String password) {
    }
}
