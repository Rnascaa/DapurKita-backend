package com.dapurkita.favorite;

import com.dapurkita.recipe.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

/** Baris dibuat lewat query native idempoten di FavoriteRepository, entitas ini untuk membaca. */
@Entity
@Table(name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() {
    }

    public FavoriteId getId() { return id; }
    public Recipe getRecipe() { return recipe; }
    public Instant getCreatedAt() { return createdAt; }
}
