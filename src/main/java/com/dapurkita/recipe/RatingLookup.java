package com.dapurkita.recipe;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Rata-rata dan jumlah ulasan untuk sekumpulan resep, satu query per halaman daftar. */
@Component
public class RatingLookup {

    private final EntityManager em;

    public RatingLookup(EntityManager em) {
        this.em = em;
    }

    public Map<Long, Rating> forRecipes(Collection<Long> recipeIds) {
        Map<Long, Rating> result = new HashMap<>();
        if (recipeIds.isEmpty()) {
            return result;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "select recipe_id, avg(rating), count(*) from reviews where recipe_id in (:ids) group by recipe_id")
                .setParameter("ids", recipeIds)
                .getResultList();
        for (Object[] row : rows) {
            double average = new BigDecimal(row[1].toString()).setScale(1, RoundingMode.HALF_UP).doubleValue();
            result.put(((Number) row[0]).longValue(), new Rating(average, ((Number) row[2]).longValue()));
        }
        return result;
    }

    public Rating forRecipe(Long recipeId) {
        return forRecipes(List.of(recipeId)).getOrDefault(recipeId, Rating.NONE);
    }
}
