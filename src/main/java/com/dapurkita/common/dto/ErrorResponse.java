package com.dapurkita.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldErrorItem> errors,
        String path,
        Instant timestamp) {
}
