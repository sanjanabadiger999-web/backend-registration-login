package com.reglogin.auth.security;

import com.reglogin.auth.entity.JwtToken;
import com.reglogin.auth.exception.TokenValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long expiryMinutes;
    private final String issuer;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiry.minutes:30}") long expiryMinutes,
            @Value("${jwt.issuer:reglogin-auth-service}") String issuer,
            @Value("${jwt.cookie.name:accessToken}") String cookieName,
            @Value("${jwt.cookie.secure:false}") boolean cookieSecure,
            @Value("${jwt.cookie.same-site:Lax}") String cookieSameSite) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET is too short: HS256 needs a secret of at least 32 bytes (256 bits) of UTF-8 text. "
                            + "Set a longer JWT_SECRET environment variable.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);

        this.expiryMinutes = expiryMinutes;
        this.issuer = issuer;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    public String generateToken(Long userId, String username) {

        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(expiryMinutes));

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .claim("uid", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new TokenValidationException("Invalid or expired token");
        }
    }

    public String getCookieName() {
        return cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public long getExpirySeconds() {
        return Duration.ofMinutes(expiryMinutes).toSeconds();
    }

    public long getExpiryMinutes() {
        return expiryMinutes;
    }
}