package com.reglogin.auth.service;

import com.reglogin.auth.dto.LoginRequest;
import com.reglogin.auth.dto.MeResponse;
import com.reglogin.auth.entity.JwtToken;
import com.reglogin.auth.exception.InvalidCredentialsException;
import com.reglogin.auth.exception.TokenValidationException;
import com.reglogin.auth.repository.JwtTokenRepository;
import com.reglogin.auth.repository.UserRepository;
import com.reglogin.auth.security.JwtService;
import com.reglogin.user.entity.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            JwtTokenRepository jwtTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult login(LoginRequest request) {

        String name = request.getName().trim();

        User user = userRepository.findByName(name)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid username or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid username or password");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getName());

        LocalDateTime now = LocalDateTime.now();

        JwtToken jwtToken = new JwtToken();
        jwtToken.setUser(user);
        jwtToken.setToken(token);
        jwtToken.setCreationTime(now);
        jwtToken.setExpiryTime(
                now.plusMinutes(jwtService.getExpiryMinutes()));

        jwtTokenRepository.save(jwtToken);

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
        }
    }
}