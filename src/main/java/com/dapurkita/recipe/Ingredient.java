package com.dapurkita.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String quantity;

    @Column(length = 30)
    private String unit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Ingredient() {
    }

    Ingredient(Recipe recipe, String name, String quantity, String unit, int sortOrder) {
        this.recipe = recipe;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.sortOrder = sortOrder;
    }

    public String getName() { return name; }
    public String getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public int getSortOrder() { return sortOrder; }
}
