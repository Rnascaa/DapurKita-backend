package com.dapurkita.recipe;

/** Ringkasan ulasan satu resep. average null jika belum ada ulasan. */
public record Rating(Double average, long count) {

    public static final Rating NONE = new Rating(null, 0);
}
