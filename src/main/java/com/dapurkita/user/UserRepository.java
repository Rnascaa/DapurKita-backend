package com.dapurkita.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Email selalu disimpan huruf kecil. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
