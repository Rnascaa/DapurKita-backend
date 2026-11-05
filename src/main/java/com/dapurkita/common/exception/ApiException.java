package com.dapurkita.common.exception;

import com.dapurkita.common.dto.FieldErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;
    private final List<FieldErrorItem> errors;

    public ApiException(HttpStatus status, ErrorCode code, String message, List<FieldErrorItem> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }

    public HttpStatus getStatus() { return status; }
    public ErrorCode getCode() { return code; }
    public List<FieldErrorItem> getErrors() { return errors; }

    public static ApiException validation(String field, String message) {
        return validation(List.of(new FieldErrorItem(field, message)));
    }

    public static ApiException validation(List<FieldErrorItem> errors) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Data tidak valid", errors);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message, null);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message, null);
    }

    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS,
                "Email atau password salah", null);
    }

    public static ApiException emailExists() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.EMAIL_ALREADY_EXISTS,
                "Email sudah terdaftar", null);
    }
}
