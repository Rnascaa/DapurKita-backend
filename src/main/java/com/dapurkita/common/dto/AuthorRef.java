package com.dapurkita.common.dto;

import com.dapurkita.user.User;

/** Nama penulis yang boleh tampil publik. Email tidak pernah ikut. */
public record AuthorRef(Long id, String name) {

    public static AuthorRef from(User user) {
        return new AuthorRef(user.getId(), user.getName());
    }
}
