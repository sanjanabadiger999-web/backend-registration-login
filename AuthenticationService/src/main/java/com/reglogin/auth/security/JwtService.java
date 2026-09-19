package com.reglogin.auth.security;

import com.reglogin.auth.config.JwtProperties;
import com.reglogin.auth.exception.TokenValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Creates a signed JWT containing subject=username, uid, iat and exp claims.
     * The expiry is taken from the configurable jwt.expiry.minutes property.
     */
    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(jwtProperties.getExpiryMinutes()));

        return Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .claim("uid", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and cryptographically validates the token. Throws TokenValidationException
     * for a malformed, tampered, or expired token. Never returns the raw secret or password.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new TokenValidationException("Invalid or expired token");
        }
    }

    public long getExpirySeconds() {
        return Duration.ofMinutes(jwtProperties.getExpiryMinutes()).toSeconds();
    }

    public String getCookieName() {
        return jwtProperties.getCookieName();
    }

    public boolean isCookieSecure() {
        return jwtProperties.isCookieSecure();
    }

    public String getCookieSameSite() {
        return jwtProperties.getCookieSameSite();
    }
}