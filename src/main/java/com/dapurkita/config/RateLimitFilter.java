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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Pembatas sederhana per IP (jendela tetap 1 menit) untuk login dan register. In-memory, satu instance. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;

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
        if (!props.rateLimit().enabled() || !"POST".equals(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !(uri.equals("/api/v1/auth/login") || uri.equals("/api/v1/auth/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean login = request.getRequestURI().equals("/api/v1/auth/login");
        int limit = login ? props.rateLimit().loginPerMinute() : props.rateLimit().registerPerMinute();
        String key = (login ? "login:" : "register:") + request.getRemoteAddr();
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
