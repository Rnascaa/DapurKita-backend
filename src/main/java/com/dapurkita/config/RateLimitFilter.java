package com.dapurkita.config;

import com.dapurkita.common.exception.ErrorCode;
import com.dapurkita.security.ErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Pembatas sederhana per IP (jendela tetap 1 menit) untuk login, register, serta menulis komentar
 * dan ulasan. In-memory, satu instance.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final Pattern COMMENTS = Pattern.compile("^/api/v1/recipes/\\d+/comments$");
    private static final Pattern MY_REVIEW = Pattern.compile("^/api/v1/recipes/\\d+/reviews/me$");

    private record Window(long startMs, AtomicInteger count) {
    }

    private final AppProperties props;
    private final ErrorWriter writer;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties props, ErrorWriter writer) {
        this.props = props;
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !props.rateLimit().enabled() || bucket(request) == null;
    }

    /** @return nama kelompok batas untuk request ini, atau null jika tidak dibatasi */
    private static String bucket(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && uri.equals("/api/v1/auth/login")) return "login";
        if ("POST".equals(method) && uri.equals("/api/v1/auth/register")) return "register";
        if (("POST".equals(method) && COMMENTS.matcher(uri).matches())
                || ("PUT".equals(method) && MY_REVIEW.matcher(uri).matches())) return "community";
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String bucket = bucket(request);
        int limit = switch (bucket) {
            case "login" -> props.rateLimit().loginPerMinute();
            case "register" -> props.rateLimit().registerPerMinute();
            default -> props.rateLimit().communityPerMinute();
        };
        String key = bucket + ":" + request.getRemoteAddr();
        long now = System.currentTimeMillis();

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now - e.getValue().startMs() > WINDOW_MS);
        }
        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.startMs() > WINDOW_MS
                        ? new Window(now, new AtomicInteger(0)) : current);
        if (window.count().incrementAndGet() > limit) {
            writer.write(request, response, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS,
                    "Terlalu banyak percobaan. Coba lagi nanti.");
            return;
        }
        chain.doFilter(request, response);
    }
}
