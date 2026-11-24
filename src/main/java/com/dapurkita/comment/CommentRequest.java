package com.dapurkita.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Komentar wajib diisi")
        @Size(max = 1000, message = "Komentar maksimal 1000 karakter")
        String content,

        Long parentId) {

    public CommentRequest {
        content = content == null ? null : content.strip();
    }
}
