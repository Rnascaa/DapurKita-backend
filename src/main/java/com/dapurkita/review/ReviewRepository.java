package com.dapurkita.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "select r from Review r join fetch r.user where r.recipe.id = :recipeId",
            countQuery = "select count(r) from Review r where r.recipe.id = :recipeId")
    Page<Review> findByRecipe(@Param("recipeId") Long recipeId, Pageable pageable);

    @Query("select r from Review r join fetch r.user where r.recipe.id = :recipeId and r.user.id = :userId")
    Optional<Review> findMine(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    @Query("select r from Review r join fetch r.user join fetch r.recipe "
            + "where r.rating >= 4 and r.content is not null order by r.createdAt desc, r.id desc")
    List<Review> findFeatured(Pageable pageable);

    @Modifying
    @Query("delete from Review r where r.recipe.id = :recipeId and r.user.id = :userId")
    void deleteMine(@Param("recipeId") Long recipeId, @Param("userId") Long userId);
}
