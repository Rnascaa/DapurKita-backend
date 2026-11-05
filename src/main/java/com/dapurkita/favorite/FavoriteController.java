package com.dapurkita.favorite;

import com.dapurkita.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipes/{id}/favorite")
public class FavoriteController {

    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @PutMapping
    public ResponseEntity<Void> add(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        service.add(CurrentUser.idOrNull(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        service.remove(CurrentUser.idOrNull(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
