package com.dapurkita.comment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = "select c from Comment c join fetch c.author where c.recipe.id = :recipeId and c.parentId is null",
            countQuery = "select count(c) from Comment c where c.recipe.id = :recipeId and c.parentId is null")
    Page<Comment> findTopLevel(@Param("recipeId") Long recipeId, Pageable pageable);

    @Query("select c from Comment c join fetch c.author where c.parentId in :parentIds order by c.createdAt asc, c.id asc")
    List<Comment> findReplies(@Param("parentIds") Collection<Long> parentIds);

    @Query("select c from Comment c join fetch c.author where c.id = :id")
    Optional<Comment> findWithAuthorById(@Param("id") Long id);

    @Query("select count(c) from Comment c where c.recipe.id = :recipeId")
    long countByRecipe(@Param("recipeId") Long recipeId);
}
