package com.dapurkita.image;

import com.dapurkita.common.exception.ApiException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageStorage storage;

    public ImageController(ImageStorage storage) {
        this.storage = storage;
    }

    /** ADMIN (dicek di SecurityConfig). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam(name = "file", required = false) MultipartFile file) {
        String name = storage.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", "/api/v1/images/" + name));
    }

    /** Publik. Nama acak dan isi tidak pernah berubah, jadi boleh di-cache selamanya. */
    @GetMapping("/{name:.+}")
    public ResponseEntity<Resource> get(@PathVariable("name") String name) {
        Path path = storage.find(name).orElseThrow(() -> ApiException.notFound("Foto tidak ditemukan"));
        return ResponseEntity.ok()
                .contentType(ImageStorage.mediaType(name))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(new FileSystemResource(path));
    }
}
