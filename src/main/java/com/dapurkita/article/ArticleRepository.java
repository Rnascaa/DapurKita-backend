package com.dapurkita.article;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Article> findAll(Specification<Article> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Article> findDetailById(Long id);

    /** Terbit tepat sebelumnya (lebih lama). Pakai PageRequest.of(0, 1). */
    @Query("select a from Article a where a.publishedAt < :at or (a.publishedAt = :at and a.id < :id) "
            + "order by a.publishedAt desc, a.id desc")
    List<Article> findOlder(@Param("at") Instant at, @Param("id") Long id, Pageable pageable);

    /** Terbit tepat sesudahnya (lebih baru). Pakai PageRequest.of(0, 1). */
    @Query("select a from Article a where a.publishedAt > :at or (a.publishedAt = :at and a.id > :id) "
            + "order by a.publishedAt asc, a.id asc")
    List<Article> findNewer(@Param("at") Instant at, @Param("id") Long id, Pageable pageable);

    @Query("select a.category, count(a) from Article a group by a.category")
    List<Object[]> countByCategory();

    @Query(value = "select tag, count(*) from article_tags group by tag order by count(*) desc, tag asc limit 20",
            nativeQuery = true)
    List<Object[]> topTags();
}
