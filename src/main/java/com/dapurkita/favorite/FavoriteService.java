package com.dapurkita.favorite;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.exception.ApiException;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.recipe.RecipeMapper;
import com.dapurkita.recipe.RecipeRepository;
import com.dapurkita.recipe.RecipeSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final RecipeRepository recipes;

    public FavoriteService(FavoriteRepository favorites, RecipeRepository recipes) {
        this.favorites = favorites;
        this.recipes = recipes;
    }

    @Transactional
    public void add(Long userId, Long recipeId) {
        requireRecipe(recipeId);
        favorites.add(userId, recipeId);
    }

    @Transactional
    public void remove(Long userId, Long recipeId) {
        requireRecipe(recipeId);
        favorites.remove(userId, recipeId);
    }

    /** Untuk favorit, newest/oldest mengikuti waktu resep difavoritkan, title mengikuti judul resep. */
    @Transactional(readOnly = true)
    public PageResponse<RecipeSummary> list(Long userId, ListParams params) {
        Sort sort = switch (params.sort()) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id.recipeId"));
            case "title" -> Sort.by(Sort.Order.asc("recipe.title"), Sort.Order.asc("id.recipeId"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id.recipeId"));
        };
        Page<Favorite> page = favorites.findPage(userId, PageRequest.of(params.page() - 1, params.size(), sort));
        return PageResponse.of(page, page.getContent().stream()
                .map(f -> RecipeMapper.toSummary(f.getRecipe(), true))
                .toList());
    }

    private void requireRecipe(Long recipeId) {
        if (!recipes.existsById(recipeId)) {
            throw ApiException.notFound("Resep tidak ditemukan");
        }
    }
}
