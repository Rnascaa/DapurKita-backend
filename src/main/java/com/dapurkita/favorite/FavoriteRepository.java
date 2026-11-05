package com.dapurkita.favorite;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    /** Idempoten: menambah favorit yang sudah ada tidak menghasilkan error. */
    @Modifying
    @Query(value = "insert into favorites (user_id, recipe_id, created_at) values (:userId, :recipeId, now()) "
            + "on conflict do nothing", nativeQuery = true)
    void add(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Modifying
    @Query("delete from Favorite f where f.id.userId = :userId and f.id.recipeId = :recipeId")
    void remove(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Query("select f.id.recipeId from Favorite f where f.id.userId = :userId and f.id.recipeId in :ids")
    Set<Long> findFavoritedIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Query(value = "select f from Favorite f join fetch f.recipe r join fetch r.category where f.id.userId = :userId",
            countQuery = "select count(f) from Favorite f where f.id.userId = :userId")
    Page<Favorite> findPage(@Param("userId") Long userId, Pageable pageable);
}
