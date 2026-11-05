package com.dapurkita.category;

/** Ringkasan kategori di dalam resep. */
public record CategoryRef(Long id, String name) {

    public static CategoryRef from(Category category) {
        return new CategoryRef(category.getId(), category.getName());
    }
}
