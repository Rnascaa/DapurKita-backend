package com.dapurkita.review;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.exception.ApiException;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.recipe.Recipe;
import com.dapurkita.recipe.RecipeRepository;
import com.dapurkita.user.UserRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    public static final int FEATURED_MAX = 10;

    private final ReviewRepository reviews;
    private final RecipeRepository recipes;
    private final UserRepository users;

    public ReviewService(ReviewRepository reviews, RecipeRepository recipes, UserRepository users) {
        this.reviews = reviews;
        this.recipes = recipes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(Long recipeId, ListParams params) {
        requireRecipe(recipeId);
        Page<Review> page = reviews.findByRecipe(recipeId, PageRequest.of(params.page() - 1, params.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(page, ReviewResponse::of);
    }

    @Transactional(readOnly = true)
    public ReviewResponse mine(Long recipeId, Long userId) {
        requireRecipe(recipeId);
        return reviews.findMine(recipeId, userId).map(ReviewResponse::of)
                .orElseThrow(() -> ApiException.notFound("Kamu belum mengulas resep ini"));
    }

    /**
     * Membuat atau mengubah ulasan milik user. Jika dua request pertama datang bersamaan,
     * salah satunya melanggar uq_reviews_recipe_user dan dijawab sebagai validasi agar bisa diulang.
     */
    @Transactional
    public ReviewResponse save(Long recipeId, ReviewRequest request, Long userId) {
        Recipe recipe = requireRecipe(recipeId);
        Review review = reviews.findMine(recipeId, userId)
                .orElseGet(() -> new Review(recipe, users.getReferenceById(userId)));
        review.change(request.rating(), request.content());
        try {
            return ReviewResponse.of(reviews.saveAndFlush(review));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.validation("rating", "Ulasan sedang disimpan, coba lagi");
        }
    }

    /** Idempoten: tetap 204 walau user belum pernah mengulas. */
    @Transactional
    public void removeMine(Long recipeId, Long userId) {
        requireRecipe(recipeId);
        reviews.deleteMine(recipeId, userId);
    }

    @Transactional
    public void remove(Long id) {
        Review review = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Ulasan tidak ditemukan"));
        reviews.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> featured(Integer size) {
        int limit = size == null ? 5 : size;
        if (limit < 1 || limit > FEATURED_MAX) {
            throw ApiException.validation("size", "Ukuran harus 1 sampai " + FEATURED_MAX);
        }
        return reviews.findFeatured(PageRequest.of(0, limit)).stream().map(ReviewResponse::featured).toList();
    }

    private Recipe requireRecipe(Long recipeId) {
        return recipes.findById(recipeId).orElseThrow(() -> ApiException.notFound("Resep tidak ditemukan"));
    }
}
