package com.reglogin.auth.service;

import com.reglogin.auth.config.JwtProperties;
import com.reglogin.auth.dto.LoginRequest;
import com.reglogin.auth.dto.MeResponse;
import com.reglogin.auth.entity.JwtToken;
import com.reglogin.auth.entity.User;
import com.reglogin.auth.exception.InvalidCredentialsException;
import com.reglogin.auth.exception.TokenValidationException;
import com.reglogin.auth.repository.JwtTokenRepository;
import com.reglogin.auth.repository.UserRepository;
import com.reglogin.auth.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       JwtTokenRepository jwtTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String name = request.getName().trim();

        User user = userRepository.findByName(name)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Always verify against the BCrypt hash; never compare plain text.
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getName());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(jwtProperties.getExpiryMinutes());

        JwtToken jwtToken = new JwtToken();
        jwtToken.setUser(user);
        jwtToken.setToken(token);
        jwtToken.setCreationTime(now);
        jwtToken.setExpiryTime(expiry);
        jwtTokenRepository.save(jwtToken);

        log.info("Login success for user id={} name={}", user.getId(), user.getName());

        return new AuthResult(token, user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(String username) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new TokenValidationException("User no longer exists"));
        return new MeResponse(user.getName(), user.getEmail(), user.getPhone());
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtTokenRepository.deleteByToken(token);
            log.info("JWT invalidated on logout");
        }
    }
}