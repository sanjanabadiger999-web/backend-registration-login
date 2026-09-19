package com.reglogin.auth.security;

import com.reglogin.auth.entity.User;
import com.reglogin.auth.repository.JwtTokenRepository;
import com.reglogin.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtTokenRepository jwtTokenRepository;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   JwtTokenRepository jwtTokenRepository,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.jwtTokenRepository = jwtTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseToken(token);

                // The token must also exist in the DB (i.e. it has not been logged out / revoked).
                if (!jwtTokenRepository.existsByToken(token)) {
                    throw new JwtException("Revoked token");
                }

                String username = claims.getSubject();
                Long userId = claims.get("uid", Long.class);

                User user = userRepository.findById(userId)
                        .filter(u -> u.getName().equals(username))
                        .orElseThrow(() -> new JwtException("User no longer exists"));

                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Invalid/expired/revoked token: leave the security context empty,
                // protected endpoints will return 401.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
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