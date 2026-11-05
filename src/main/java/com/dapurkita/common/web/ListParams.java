package com.dapurkita.common.web;

import com.dapurkita.common.dto.FieldErrorItem;
import com.dapurkita.common.exception.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parameter pagination dan sort. Nilai di luar batas ditolak dengan 400 VALIDATION_ERROR
 * (field = nama parameter), tidak dibulatkan diam-diam.
 */
public record ListParams(int page, int size, String sort) {

    public static final int DEFAULT_SIZE = 12;
    public static final int MAX_SIZE = 50;
    private static final Set<String> SORTS = Set.of("newest", "oldest", "title");

    public static ListParams parse(Integer page, Integer size, String sort) {
        List<FieldErrorItem> errors = new ArrayList<>();
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        String order = sort == null || sort.isBlank() ? "newest" : sort;

        if (p < 1) {
            errors.add(new FieldErrorItem("page", "Halaman minimal 1"));
        }
        if (s < 1 || s > MAX_SIZE) {
            errors.add(new FieldErrorItem("size", "Ukuran halaman harus 1 sampai " + MAX_SIZE));
        }
        if (!SORTS.contains(order)) {
            errors.add(new FieldErrorItem("sort", "Urutan harus newest, oldest, atau title"));
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return new ListParams(p, s, order);
    }
}
