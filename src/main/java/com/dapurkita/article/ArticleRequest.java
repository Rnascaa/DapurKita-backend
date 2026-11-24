package com.dapurkita.article;

import com.dapurkita.common.web.ImageUrls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Body POST dan PUT /articles. Tag di-trim, huruf kecil, dan duplikat dibuang. */
public record ArticleRequest(
        @NotBlank(message = "Judul wajib diisi")
        @Size(min = 3, max = 150, message = "Judul harus 3 sampai 150 karakter")
        String title,

        @Size(max = 300, message = "Ringkasan maksimal 300 karakter")
        String excerpt,

        @NotBlank(message = "Isi artikel wajib diisi")
        @Size(max = 20000, message = "Isi artikel maksimal 20000 karakter")
        String content,

        @Size(max = 2048, message = "URL gambar maksimal 2048 karakter")
        @Pattern(regexp = ImageUrls.PATTERN, message = ImageUrls.MESSAGE)
        String coverImageUrl,

        @NotNull(message = "Kategori wajib diisi")
        ArticleCategory category,

        @Size(max = 8, message = "Maksimal 8 tag")
        List<@NotBlank(message = "Tag tidak boleh kosong") @Size(max = 30, message = "Tag maksimal 30 karakter") String> tags) {

    public ArticleRequest {
        title = title == null ? null : title.strip();
        excerpt = excerpt == null || excerpt.isBlank() ? null : excerpt.strip();
        content = content == null ? null : content.strip();
        coverImageUrl = coverImageUrl == null || coverImageUrl.isBlank() ? null : coverImageUrl.strip();
        if (tags == null) {
            tags = List.of();
        } else {
            // Tag kosong dibiarkan di tempatnya agar @NotBlank melaporkannya; duplikat dibuang.
            Set<String> seen = new HashSet<>();
            List<String> normalized = new ArrayList<>();
            for (String tag : tags) {
                String value = tag == null ? "" : tag.strip().toLowerCase(Locale.ROOT);
                if (value.isEmpty() || seen.add(value)) {
                    normalized.add(value);
                }
            }
            tags = List.copyOf(normalized);
        }
    }
}
