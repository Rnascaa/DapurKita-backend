package com.dapurkita.comment;

import com.dapurkita.common.dto.AuthorRef;
import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        Long recipeId,
        Long parentId,
        AuthorRef author,
        String content,
        Instant createdAt,
        List<CommentResponse> replies) {

    static CommentResponse of(Comment c, Long recipeId, List<CommentResponse> replies) {
        return new CommentResponse(c.getId(), recipeId, c.getParentId(), AuthorRef.from(c.getAuthor()),
                c.getContent(), c.getCreatedAt(), replies);
    }
}
