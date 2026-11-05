package com.dapurkita.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Pagination 1-based sesuai kontrak. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <E, T> PageResponse<T> of(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber() + 1,
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasNext());
    }

    public static <T> PageResponse<T> of(Page<?> source, List<T> content) {
        return new PageResponse<>(
                content,
                source.getNumber() + 1,
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasNext());
    }
}
