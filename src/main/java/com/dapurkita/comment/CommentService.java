package com.dapurkita.comment;

import com.dapurkita.common.exception.ApiException;
import com.dapurkita.common.exception.ErrorCode;
import com.dapurkita.common.web.ListParams;
import com.dapurkita.recipe.Recipe;
import com.dapurkita.recipe.RecipeRepository;
import com.dapurkita.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final RecipeRepository recipes;
    private final UserRepository users;

    public CommentService(CommentRepository comments, RecipeRepository recipes, UserRepository users) {
        this.comments = comments;
        this.recipes = recipes;
        this.users = users;
    }

    /** Komentar utama terbaru lebih dulu; balasan terlama lebih dulu. */
    @Transactional(readOnly = true)
    public CommentPage list(Long recipeId, ListParams params) {
        requireRecipe(recipeId);
        Page<Comment> page = comments.findTopLevel(recipeId, PageRequest.of(params.page() - 1, params.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        List<Long> ids = page.getContent().stream().map(Comment::getId).toList();
        Map<Long, List<CommentResponse>> replies = ids.isEmpty() ? Map.of() : comments.findReplies(ids).stream()
                .collect(Collectors.groupingBy(Comment::getParentId,
                        Collectors.mapping(c -> CommentResponse.of(c, recipeId, List.of()), Collectors.toList())));
        List<CommentResponse> content = page.getContent().stream()
                .map(c -> CommentResponse.of(c, recipeId, replies.getOrDefault(c.getId(), List.of())))
                .toList();
        return new CommentPage(content, page.getNumber() + 1, page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.hasNext(), comments.countByRecipe(recipeId));
    }

    @Transactional
    public CommentResponse create(Long recipeId, CommentRequest request, Long userId) {
        Recipe recipe = requireRecipe(recipeId);
        if (request.parentId() != null) {
            Comment parent = comments.findById(request.parentId()).orElse(null);
            if (parent == null || !parent.getRecipe().getId().equals(recipeId) || parent.getParentId() != null) {
                throw ApiException.validation("parentId", "Komentar yang dibalas tidak ditemukan");
            }
        }
        Comment saved = comments.saveAndFlush(new Comment(recipe, request.parentId(), users.getReferenceById(userId),
                request.content()));
        return CommentResponse.of(saved, recipeId, List.of());
    }

    /** Pemilik atau ADMIN. Balasan ikut terhapus lewat ON DELETE CASCADE. */
    @Transactional
    public void delete(Long id, Long userId, boolean admin) {
        Comment comment = comments.findWithAuthorById(id)
                .orElseThrow(() -> ApiException.notFound("Komentar tidak ditemukan"));
        if (!admin && !comment.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Hanya penulis atau admin yang dapat menghapus komentar ini", null);
        }
        comments.delete(comment);
    }

    private Recipe requireRecipe(Long recipeId) {
        return recipes.findById(recipeId).orElseThrow(() -> ApiException.notFound("Resep tidak ditemukan"));
    }
}
