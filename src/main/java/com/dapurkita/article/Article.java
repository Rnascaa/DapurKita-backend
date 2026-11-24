package com.dapurkita.article;

import com.dapurkita.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "articles")
public class Article {

    static final int WORDS_PER_MINUTE = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 300)
    private String excerpt;

    @Column(nullable = false, length = 20000)
    private String content;

    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ArticleCategory category;

    @Column(name = "reading_time_minutes", nullable = false)
    private int readingTimeMinutes;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Huruf kecil, tanpa duplikat, urutan sesuai request. BatchSize mencegah N+1 di daftar. */
    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "tag", nullable = false, length = 30)
    @BatchSize(size = 50)
    private List<String> tags = new ArrayList<>();

    protected Article() {
    }

    Article(User author) {
        this.author = author;
    }

    @PrePersist
    void onCreate() {
        Instant now = now();
        publishedAt = now;
        updatedAt = now;
    }

    /** Semantik PUT: semua field diganti, publishedAt tetap. */
    void replaceContent(ArticleRequest request) {
        this.title = request.title();
        this.excerpt = request.excerpt();
        this.content = request.content();
        this.coverImageUrl = request.coverImageUrl();
        this.category = request.category();
        this.readingTimeMinutes = readingTime(request.content());
        this.updatedAt = now();
        tags.clear();
        tags.addAll(request.tags());
    }

    static int readingTime(String content) {
        String text = content.strip();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        return Math.max(1, (words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getExcerpt() { return excerpt; }
    public String getContent() { return content; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public ArticleCategory getCategory() { return category; }
    public int getReadingTimeMinutes() { return readingTimeMinutes; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<String> getTags() { return tags; }
}
