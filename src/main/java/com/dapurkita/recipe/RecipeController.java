package com.dapurkita.recipe;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.security.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    /** Publik. Jika token dikirim, isFavorited terisi sesuai user. */
    @GetMapping
    public PageResponse<RecipeSummary> list(@RequestParam(name = "q", required = false) String q,
                                            @RequestParam(name = "categoryId", required = false) Long categoryId,
                                            @RequestParam(name = "ingredient", required = false) List<String> ingredient,
                                            @RequestParam(name = "page", required = false) Integer page,
                                            @RequestParam(name = "size", required = false) Integer size,
                                            @RequestParam(name = "sort", required = false) String sort,
                                            @AuthenticationPrincipal Jwt jwt) {
        return service.list(q, categoryId, ingredient, ListParams.parse(page, size, sort),
                CurrentUser.idOrNull(jwt));
    }

    @GetMapping("/{id}")
    public RecipeDetail get(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, CurrentUser.idOrNull(jwt));
    }

    @PostMapping
    public ResponseEntity<RecipeDetail> create(@Valid @RequestBody RecipeRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        RecipeDetail created = service.create(request, CurrentUser.idOrNull(jwt));
        return ResponseEntity.created(URI.create("/api/v1/recipes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public RecipeDetail update(@PathVariable("id") Long id, @Valid @RequestBody RecipeRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, CurrentUser.idOrNull(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
