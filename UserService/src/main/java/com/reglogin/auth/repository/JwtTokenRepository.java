package com.reglogin.auth.repository;

import com.reglogin.auth.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByToken(String token);

    boolean existsByToken(String token);

    void deleteByToken(String token);
}