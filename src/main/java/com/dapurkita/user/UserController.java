package com.dapurkita.user;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.favorite.FavoriteService;
import com.dapurkita.recipe.RecipeSummary;
import com.dapurkita.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService, FavoriteService favoriteService) {
        this.userService = userService;
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.get(CurrentUser.idOrNull(jwt));
    }

    @PatchMapping
    public UserResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateName(CurrentUser.idOrNull(jwt), request.name());
    }

    @GetMapping("/favorites")
    public PageResponse<RecipeSummary> favorites(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size,
                                                 @RequestParam(required = false) String sort) {
        return favoriteService.list(CurrentUser.idOrNull(jwt), ListParams.parse(page, size, sort));
    }
}
