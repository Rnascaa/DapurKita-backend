package com.dapurkita.recipe;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    /** Kategori ikut dimuat agar tidak terjadi N+1 saat membangun daftar. */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Recipe> findAll(Specification<Recipe> spec, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Recipe> findWithCategoryById(Long id);
}
