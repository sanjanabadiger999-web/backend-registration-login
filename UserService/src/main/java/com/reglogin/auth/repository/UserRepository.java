package com.reglogin.auth.repository;

import com.reglogin.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Single User repository for both registration and authentication.
 * (Registration lives in com.reglogin.user but uses this repository so there is
 * one consistent User entity and one repository across the whole app.)
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByName(String name);
}