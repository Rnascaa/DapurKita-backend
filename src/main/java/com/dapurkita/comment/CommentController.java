package com.dapurkita.comment;

import com.dapurkita.common.web.ListParams;
import com.dapurkita.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/recipes/{id}/comments")
    public CommentPage list(@PathVariable("id") Long id,
                            @RequestParam(name = "page", required = false) Integer page,
                            @RequestParam(name = "size", required = false) Integer size) {
        return service.list(id, ListParams.parse(page, size, null));
    }

    @PostMapping("/api/v1/recipes/{id}/comments")
    public ResponseEntity<CommentResponse> create(@PathVariable("id") Long id,
                                                  @Valid @RequestBody CommentRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id, request, CurrentUser.idOrNull(jwt)));
    }

    @DeleteMapping("/api/v1/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, CurrentUser.idOrNull(jwt), CurrentUser.isAdmin(jwt));
        return ResponseEntity.noContent().build();
    }
}
