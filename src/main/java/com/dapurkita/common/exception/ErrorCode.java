package com.dapurkita.common.exception;

/** Kode error stabil. Frontend bercabang berdasarkan kode ini, bukan teks pesan. */
public enum ErrorCode {
    VALIDATION_ERROR,
    MALFORMED_REQUEST,
    INVALID_CREDENTIALS,
    TOKEN_EXPIRED,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    EMAIL_ALREADY_EXISTS,
    TOO_MANY_REQUESTS,
    INTERNAL_ERROR
}
