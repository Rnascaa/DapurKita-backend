package com.dapurkita.comment;

import java.util.List;

/** Bentuk PageResponse ditambah totalComments (komentar utama beserta balasan). */
public record CommentPage(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        long totalComments) {
}
