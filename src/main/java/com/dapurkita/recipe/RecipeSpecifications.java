package com.dapurkita.recipe;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    static Specification<Recipe> build(String q, Long categoryId, List<String> ingredients) {
        Specification<Recipe> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            spec = spec.and(titleContains(q.strip()));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (ingredients != null) {
            for (String term : ingredients) {
                if (term != null && !term.isBlank()) {
                    spec = spec.and(hasIngredient(term.strip()));
                }
            }
        }
        return spec;
    }

    private static Specification<Recipe> titleContains(String q) {
        String pattern = "%" + escapeLike(q.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }

    /**
     * Satu EXISTS per bahan, digabung dengan AND: semua bahan yang diminta harus cocok.
     * Pencocokan di awal kata (kontrak v2.3.1): "ayam" cocok dengan "Daging ayam" tetapi
     * tidak dengan "Bayam"; awalan seperti "sant" tetap cocok dengan "Santan".
     */
    private static Specification<Recipe> hasIngredient(String term) {
        String t = escapeLike(term.toLowerCase(Locale.ROOT));
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Ingredient> ingredient = sub.from(Ingredient.class);
            Expression<String> name = cb.lower(ingredient.get("name"));
            Predicate wordStart = cb.or(
                    cb.like(name, t + "%", '\\'),
                    cb.like(name, "% " + t + "%", '\\'),
                    cb.like(name, "%(" + t + "%", '\\'),
                    cb.like(name, "%-" + t + "%", '\\'),
                    cb.like(name, "%/" + t + "%", '\\'));
            sub.select(cb.literal(1L)).where(cb.equal(ingredient.get("recipe"), root), wordStart);
            return cb.exists(sub);
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
