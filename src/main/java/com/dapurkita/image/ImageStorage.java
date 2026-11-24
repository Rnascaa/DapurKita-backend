package com.dapurkita.image;

import com.dapurkita.common.exception.ApiException;
import com.dapurkita.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Foto hasil upload admin disimpan di disk (app.images.dir) dengan nama acak 32 hex.
 * Jenis file ditentukan dari isi (magic bytes), bukan dari nama atau Content-Type kiriman.
 */
@Component
public class ImageStorage {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final Pattern NAME = Pattern.compile("^[a-f0-9]{32}\\.(jpg|png|webp)$");

    private final SecureRandom random = new SecureRandom();
    private final Path dir;

    public ImageStorage(AppProperties props) {
        this.dir = Path.of(props.images().dir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Folder foto tidak bisa dibuat: " + dir, e);
        }
    }

    /** @return nama file yang disimpan, contoh 3f2a...7d.jpg */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("file", "Foto wajib diisi");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.validation("file", "Ukuran foto maksimal 5 MB");
        }
        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = detect(in.readNBytes(12))
                    .orElseThrow(() -> ApiException.validation("file", "Foto harus berformat JPEG, PNG, atau WebP"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] id = new byte[16];
        random.nextBytes(id);
        String name = HexFormat.of().formatHex(id) + "." + extension;
        Path temp = null;
        try (InputStream in = file.getInputStream()) {
            temp = Files.createTempFile(dir, "upload-", ".tmp");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, dir.resolve(name), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new UncheckedIOException(e);
        }
        return name;
    }

    /** Path aman untuk nama yang sudah lolos pola NAME; kosong jika tidak ada. */
    public Optional<Path> find(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        Path path = dir.resolve(name);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public static MediaType mediaType(String name) {
        if (name.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (name.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    static Optional<String> detect(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return Optional.of("png");
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // file sementara; aman diabaikan
        }
    }
}
