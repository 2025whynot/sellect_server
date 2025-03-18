package com.sellect.server.common.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final Long ACCESS_TOKEN_EXPIRATION_TIME;
    private final SecretKey secretKey;


    public JwtUtil(@Value("${jwt.secret}") String SECRET_KEY, @Value("${jwt.expiration}") Long EXPIRATION_TIME) {
        this.secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        this.ACCESS_TOKEN_EXPIRATION_TIME = EXPIRATION_TIME;
    }


    public String generateAccessToken(String uuid, String role) {
        return Jwts.builder()
            .setSubject(uuid)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    // User uuid가 아닌 User id를 payload에 넣도록 설정
    public String generateAccessToken(Long userId, String role) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }


    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUuid(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        String subject = extractClaims(token).getSubject();
        return Long.parseLong(subject);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    @Deprecated
    public String extractTokenFromCookie(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        return token;
    }

    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
