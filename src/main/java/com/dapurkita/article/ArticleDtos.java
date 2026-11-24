package com.dapurkita.article;

import com.dapurkita.common.dto.AuthorRef;
import java.time.Instant;
import java.util.List;

/** Bentuk respons artikel sesuai kontrak v2.4 (ArticleSummary, ArticleDetail, ArticleLink, ArticleFacets). */
public final class ArticleDtos {

    private ArticleDtos() {
    }

    public record Summary(
            Long id,
            String title,
            String excerpt,
            String coverImageUrl,
            ArticleCategory category,
            List<String> tags,
            AuthorRef author,
            int readingTimeMinutes,
            Instant publishedAt) {
    }

    public record Detail(
            Long id,
            String title,
            String excerpt,
            String coverImageUrl,
            ArticleCategory category,
            List<String> tags,
            AuthorRef author,
            int readingTimeMinutes,
            Instant publishedAt,
            String content,
            Instant updatedAt,
            Link previous,
            Link next) {
    }

    public record Link(Long id, String title, String coverImageUrl) {

        static Link of(Article a) {
            return a == null ? null : new Link(a.getId(), a.getTitle(), a.getCoverImageUrl());
        }
    }

    public record Facets(List<CategoryCount> categories, List<TagCount> tags) {
    }

    public record CategoryCount(ArticleCategory category, long count) {
    }

    public record TagCount(String name, long count) {
    }

    static Summary summary(Article a) {
        return new Summary(a.getId(), a.getTitle(), a.getExcerpt(), a.getCoverImageUrl(), a.getCategory(),
                List.copyOf(a.getTags()), AuthorRef.from(a.getAuthor()), a.getReadingTimeMinutes(), a.getPublishedAt());
    }

    static Detail detail(Article a, Article previous, Article next) {
        return new Detail(a.getId(), a.getTitle(), a.getExcerpt(), a.getCoverImageUrl(), a.getCategory(),
                List.copyOf(a.getTags()), AuthorRef.from(a.getAuthor()), a.getReadingTimeMinutes(), a.getPublishedAt(),
                a.getContent(), a.getUpdatedAt(), Link.of(previous), Link.of(next));
    }
}
