package com.dapurkita.review;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/recipes/{id}/reviews")
    public PageResponse<ReviewResponse> list(@PathVariable("id") Long id,
                                             @RequestParam(name = "page", required = false) Integer page,
                                             @RequestParam(name = "size", required = false) Integer size) {
        return service.list(id, ListParams.parse(page, size, null));
    }

    @GetMapping("/api/v1/recipes/{id}/reviews/me")
    public ReviewResponse mine(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.mine(id, CurrentUser.idOrNull(jwt));
    }

    @PutMapping("/api/v1/recipes/{id}/reviews/me")
    public ReviewResponse save(@PathVariable("id") Long id, @Valid @RequestBody ReviewRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.save(id, request, CurrentUser.idOrNull(jwt));
    }

    @DeleteMapping("/api/v1/recipes/{id}/reviews/me")
    public ResponseEntity<Void> removeMine(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        service.removeMine(id, CurrentUser.idOrNull(jwt));
        return ResponseEntity.noContent().build();
    }

    /** Moderasi; dibatasi ADMIN di SecurityConfig. */
    @DeleteMapping("/api/v1/reviews/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") Long id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/reviews/featured")
    public List<ReviewResponse> featured(@RequestParam(name = "size", required = false) Integer size) {
        return service.featured(size);
    }
}
