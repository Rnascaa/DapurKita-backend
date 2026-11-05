package com.dapurkita.security;

import com.dapurkita.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 401 (TOKEN_EXPIRED / UNAUTHORIZED) dan 403 (FORBIDDEN) dalam format error kontrak. */
@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ErrorWriter writer;

    public SecurityErrorHandlers(ErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        if (isExpired(ex)) {
            writer.write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED,
                    "Token kedaluwarsa");
        } else {
            writer.write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    "Autentikasi diperlukan");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        writer.write(request, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Akses ditolak");
    }

    private boolean isExpired(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JwtValidationException jve) {
                for (OAuth2Error error : jve.getErrors()) {
                    String description = error.getDescription();
                    if (description != null && description.toLowerCase().contains("expired")) {
                        return true;
                    }
                }
            }
            String message = t.getMessage();
            if (message != null && message.toLowerCase().contains("jwt expired")) {
                return true;
            }
        }
        return false;
    }
}
