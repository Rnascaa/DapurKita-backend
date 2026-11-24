package com.dapurkita.review;

import com.dapurkita.common.dto.AuthorRef;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** Review; untuk /reviews/featured field recipe ikut diisi (FeaturedReview). */
public record ReviewResponse(
        Long id,
        int rating,
        String content,
        AuthorRef author,
        Instant createdAt,
        Instant updatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) RecipeLink recipe) {

    public record RecipeLink(Long id, String title) {
    }

    static ReviewResponse of(Review r) {
        return new ReviewResponse(r.getId(), r.getRating(), r.getContent(), AuthorRef.from(r.getUser()),
                r.getCreatedAt(), r.getUpdatedAt(), null);
    }

    static ReviewResponse featured(Review r) {
        return new ReviewResponse(r.getId(), r.getRating(), r.getContent(), AuthorRef.from(r.getUser()),
                r.getCreatedAt(), r.getUpdatedAt(), new RecipeLink(r.getRecipe().getId(), r.getRecipe().getTitle()));
    }
}
