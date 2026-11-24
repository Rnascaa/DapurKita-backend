package com.dapurkita.common.web;

/** Aturan URL gambar resep dan sampul artikel (kontrak v2.4: skema ImageUrl). */
public final class ImageUrls {

    /** URL http/https, atau path hasil POST /images. */
    public static final String PATTERN =
            "^((?i:https?)://\\S+|/api/v1/images/[a-f0-9]{32}\\.(jpg|png|webp))$";

    public static final String MESSAGE = "URL gambar harus berawalan http/https atau hasil unggah foto";

    private ImageUrls() {
    }
}
