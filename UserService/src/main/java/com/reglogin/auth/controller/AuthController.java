package com.reglogin.auth.controller;

import com.reglogin.auth.dto.LoginRequest;
import com.reglogin.auth.dto.LoginResponse;
import com.reglogin.auth.dto.MeResponse;
import com.reglogin.auth.security.JwtService;
import com.reglogin.auth.service.AuthResult;
import com.reglogin.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthResult result = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from(jwtService.getCookieName(), result.token())
                .httpOnly(true)
                .secure(jwtService.isCookieSecure())
                .sameSite(jwtService.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.getExpirySeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginResponse("Login successful", result.user().getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(authService.me(username));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                      HttpServletResponse response) {
        String token = extractToken(request);
        authService.logout(token);

        ResponseCookie clear = ResponseCookie.from(jwtService.getCookieName(), "")
                .httpOnly(true)
                .secure(jwtService.isCookieSecure())
                .sameSite(jwtService.getCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (jwtService.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}