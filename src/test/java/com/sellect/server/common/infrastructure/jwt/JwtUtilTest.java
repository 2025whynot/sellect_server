package com.sellect.server.common.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private SecretKey secretKey;
    private static final String TEST_SECRET_KEY = "test-secret-key-123456789012345678901234567890";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 15 * 60 * 1000L; // 15분

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        jwtUtil = new JwtUtil(TEST_SECRET_KEY);
    }

    @Nested
    @DisplayName("generateAccessToken()은")
    class GenerateAccessToken {
        @Test
        @DisplayName("성공적으로 토큰을 발급한다")
        void _willSuccess() {
            // given
            String uuid = "123e4567-e89b-12d3-a456-426614174000";
            String role = "USER";

            // when
            String accessToken = jwtUtil.generateAccessToken(uuid, role);

            // then
            assertThat(accessToken).isNotBlank();

            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
            assertThat(claims.getSubject()).isEqualTo(uuid);
            assertThat(claims.get("role")).isEqualTo(role);
            assertThat(claims.getExpiration()).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("isTokenValid()는")
    class IsTokenValid {
        @Test
        @DisplayName("유효한 토큰이면 true를 반환한다")
        void willReturnTrueForValidToken() {
            // given
            String uuid = "123e4567-e89b-12d3-a456-426614174000";
            String role = "USER";
            String token = jwtUtil.generateAccessToken(uuid, role);

            // when
            boolean isValid = jwtUtil.isTokenValid(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 토큰이면 false를 반환한다")
        void willReturnFalseForInvalidToken() {
            // given
            String invalidToken = "invalid.token.value";

            // when
            boolean isValid = jwtUtil.isTokenValid(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("extractUuid()는")
    class ExtractUuid {
        @Test
        @DisplayName("토큰에서 UUID를 추출한다")
        void willExtractUuid() {
            // given
            String uuid = "123e4567-e89b-12d3-a456-426614174000";
            String role = "USER";
            String token = jwtUtil.generateAccessToken(uuid, role);

            // when
            String extractedUuid = jwtUtil.extractUuid(token);

            // then
            assertThat(extractedUuid).isEqualTo(uuid);
        }
    }

    @Nested
    @DisplayName("extractRole()는")
    class ExtractRole {
        @Test
        @DisplayName("토큰에서 역할(Role)을 추출한다")
        void willExtractRole() {
            // given
            String uuid = "123e4567-e89b-12d3-a456-426614174000";
            String role = "SELLER";
            String token = jwtUtil.generateAccessToken(uuid, role);

            // when
            String extractedRole = jwtUtil.extractRole(token);

            // then
            assertThat(extractedRole).isEqualTo(role);
        }
    }

    @Nested
    @DisplayName("extractClaims()는")
    class ExtractClaims {
        @Test
        @DisplayName("토큰에서 Claims를 추출한다")
        void willExtractClaims() {
            // given
            String uuid = "123e4567-e89b-12d3-a456-426614174000";
            String role = "USER";
            String token = jwtUtil.generateAccessToken(uuid, role);

            // when
            Claims claims = jwtUtil.extractClaims(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(uuid);
            assertThat(claims.get("role", String.class)).isEqualTo(role);
            assertThat(claims.getExpiration()).isAfter(new Date());
        }
    }
}
