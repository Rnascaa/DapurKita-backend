package com.dapurkita.recipe;

import com.dapurkita.category.Category;
import com.dapurkita.category.CategoryRepository;
import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.exception.ApiException;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.favorite.FavoriteRepository;
import com.dapurkita.user.User;
import com.dapurkita.user.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final FavoriteRepository favorites;

    public RecipeService(RecipeRepository recipes, CategoryRepository categories, UserRepository users,
                         FavoriteRepository favorites) {
        this.recipes = recipes;
        this.categories = categories;
        this.users = users;
        this.favorites = favorites;
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummary> list(String q, Long categoryId, List<String> ingredients,
                                            ListParams params, Long userId) {
        Page<Recipe> page = recipes.findAll(RecipeSpecifications.build(q, categoryId, ingredients),
                PageRequest.of(params.page() - 1, params.size(), sortFor(params.sort())));
        Set<Long> favorited = favoritedIds(userId, page.getContent().stream().map(Recipe::getId).toList());
        return PageResponse.of(page, page.getContent().stream()
                .map(r -> RecipeMapper.toSummary(r, favorited.contains(r.getId())))
                .toList());
    }

    @Transactional(readOnly = true)
    public RecipeDetail get(Long id, Long userId) {
        Recipe recipe = load(id);
        return RecipeMapper.toDetail(recipe, !favoritedIds(userId, List.of(id)).isEmpty());
    }

    @Transactional
    public RecipeDetail create(RecipeRequest request, Long adminId) {
        Category category = loadCategory(request.categoryId());
        User author = users.getReferenceById(adminId);
        Recipe recipe = new Recipe(author, category);
        recipe.replaceContent(category, request);
        recipes.save(recipe);
        return RecipeMapper.toDetail(recipe, false);
    }

    @Transactional
    public RecipeDetail update(Long id, RecipeRequest request, Long adminId) {
        Recipe recipe = load(id);
        recipe.replaceContent(loadCategory(request.categoryId()), request);
        return RecipeMapper.toDetail(recipe, !favoritedIds(adminId, List.of(id)).isEmpty());
    }

    @Transactional
    public void delete(Long id) {
        recipes.delete(load(id));
    }

    private Recipe load(Long id) {
        return recipes.findWithCategoryById(id)
                .orElseThrow(() -> ApiException.notFound("Resep tidak ditemukan"));
    }

    private Category loadCategory(Long categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> ApiException.validation("categoryId", "Kategori tidak ditemukan"));
    }

    private Set<Long> favoritedIds(Long userId, Collection<Long> recipeIds) {
        if (userId == null || recipeIds.isEmpty()) {
            return Set.of();
        }
        return favorites.findFavoritedIds(userId, recipeIds);
    }

    private static Sort sortFor(String sort) {
        return switch (sort) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case "title" -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }
}
