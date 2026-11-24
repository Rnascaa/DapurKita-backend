package com.dapurkita.article;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.security.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Baca publik; tulis hanya ADMIN (dicek di SecurityConfig). */
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ArticleDtos.Summary> list(@RequestParam(name = "q", required = false) String q,
                                                  @RequestParam(name = "category", required = false) ArticleCategory category,
                                                  @RequestParam(name = "tag", required = false) String tag,
                                                  @RequestParam(name = "page", required = false) Integer page,
                                                  @RequestParam(name = "size", required = false) Integer size) {
        return service.list(q, category, tag, ListParams.parse(page, size, null));
    }

    @GetMapping("/facets")
    public ArticleDtos.Facets facets() {
        return service.facets();
    }

    @GetMapping("/{id}")
    public ArticleDtos.Detail get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ArticleDtos.Detail> create(@Valid @RequestBody ArticleRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        ArticleDtos.Detail created = service.create(request, CurrentUser.idOrNull(jwt));
        return ResponseEntity.created(URI.create("/api/v1/articles/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ArticleDtos.Detail update(@PathVariable("id") Long id, @Valid @RequestBody ArticleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
