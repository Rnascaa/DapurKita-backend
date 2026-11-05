package com.dapurkita.recipe;

import com.dapurkita.category.Category;
import com.dapurkita.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Admin pembuat resep. Dicatat untuk audit, tidak ditampilkan di API. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "cooking_time_minutes", nullable = false)
    private int cookingTimeMinutes;

    @Column(nullable = false)
    private int servings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Ingredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    private List<Instruction> instructions = new ArrayList<>();

    protected Recipe() {
    }

    public Recipe(User author, Category category) {
        this.author = author;
        this.category = category;
    }

    @PrePersist
    void onCreate() {
        Instant now = now();
        createdAt = now;
        updatedAt = now;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** Mengganti seluruh isi resep (semantik PUT). Urutan bahan dan langkah = urutan list. */
    void replaceContent(Category category, RecipeRequest request) {
        this.category = category;
        this.title = request.title();
        this.description = request.description();
        this.imageUrl = request.imageUrl();
        this.cookingTimeMinutes = request.cookingTimeMinutes();
        this.servings = request.servings();
        this.updatedAt = now();

        ingredients.clear();
        for (int i = 0; i < request.ingredients().size(); i++) {
            IngredientRequest item = request.ingredients().get(i);
            ingredients.add(new Ingredient(this, item.name(), item.quantity(), item.unit(), i));
        }
        instructions.clear();
        for (int i = 0; i < request.instructions().size(); i++) {
            instructions.add(new Instruction(this, i + 1, request.instructions().get(i).description()));
        }
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getCookingTimeMinutes() { return cookingTimeMinutes; }
    public int getServings() { return servings; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public List<Instruction> getInstructions() { return instructions; }
}
