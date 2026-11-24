package com.dapurkita.article;

import com.dapurkita.common.dto.PageResponse;
import com.dapurkita.common.exception.ApiException;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.user.UserRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleService {

    private static final PageRequest FIRST = PageRequest.of(0, 1);

    private final ArticleRepository articles;
    private final UserRepository users;

    public ArticleService(ArticleRepository articles, UserRepository users) {
        this.articles = articles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleDtos.Summary> list(String q, ArticleCategory category, String tag, ListParams params) {
        Page<Article> page = articles.findAll(filter(q, category, tag), PageRequest.of(params.page() - 1,
                params.size(), Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))));
        return PageResponse.of(page, ArticleDtos::summary);
    }

    @Transactional(readOnly = true)
    public ArticleDtos.Detail get(Long id) {
        return detail(load(id));
    }

    @Transactional
    public ArticleDtos.Detail create(ArticleRequest request, Long adminId) {
        Article article = new Article(users.getReferenceById(adminId));
        article.replaceContent(request);
        articles.saveAndFlush(article);
        return detail(article);
    }

    @Transactional
    public ArticleDtos.Detail update(Long id, ArticleRequest request) {
        Article article = load(id);
        article.replaceContent(request);
        articles.flush();
        return detail(article);
    }

    @Transactional
    public void delete(Long id) {
        articles.delete(load(id));
    }

    /** Kategori tanpa artikel tetap dikirim dengan count 0, dalam urutan enum. */
    @Transactional(readOnly = true)
    public ArticleDtos.Facets facets() {
        Map<ArticleCategory, Long> counts = new EnumMap<>(ArticleCategory.class);
        for (Object[] row : articles.countByCategory()) {
            counts.put((ArticleCategory) row[0], ((Number) row[1]).longValue());
        }
        List<ArticleDtos.CategoryCount> categories = Arrays.stream(ArticleCategory.values())
                .map(c -> new ArticleDtos.CategoryCount(c, counts.getOrDefault(c, 0L)))
                .toList();
        List<ArticleDtos.TagCount> tags = articles.topTags().stream()
                .map(row -> new ArticleDtos.TagCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
        return new ArticleDtos.Facets(categories, tags);
    }

    private ArticleDtos.Detail detail(Article article) {
        Article previous = articles.findOlder(article.getPublishedAt(), article.getId(), FIRST).stream()
                .findFirst().orElse(null);
        Article next = articles.findNewer(article.getPublishedAt(), article.getId(), FIRST).stream()
                .findFirst().orElse(null);
        return ArticleDtos.detail(article, previous, next);
    }

    private Article load(Long id) {
        return articles.findDetailById(id).orElseThrow(() -> ApiException.notFound("Artikel tidak ditemukan"));
    }

    private static Specification<Article> filter(String q, ArticleCategory category, String tag) {
        Specification<Article> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (tag != null && !tag.isBlank()) {
            String value = tag.strip().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.isMember(value, root.get("tags")));
        }
        return spec;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
