package com.dapurkita.user;

import com.dapurkita.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse updateName(Long userId, String name) {
        User user = load(userId);
        user.setName(name);
        return UserResponse.from(user);
    }

    private User load(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Autentikasi diperlukan"));
    }
}
