package com.sellect.server.common.resolver;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.infrastructure.annotation.AuthSeller;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import java.lang.annotation.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationResolverTest {

    private AuthenticationResolver authenticationResolver;

    @BeforeEach
    void setUp() {
        authenticationResolver = new AuthenticationResolver();
    }

    @Test
    @DisplayName("@AuthUser를 사용하면 Role.USER를 가진 User를 반환한다")
    void resolveAuthUser() throws Exception {
        // given
        User user = User.builder()
            .id(1L)
            .uuid("uuid-user")
            .nickname("userNickname")
            .role(Role.USER)
            .createdAt(null)
            .updatedAt(null)
            .deletedAt(null)
            .build();
        mockAuthentication(user);
        MethodParameter methodParameter = mockMethodParameter(AuthUser.class);

        // when
        Object resolvedUser = authenticationResolver.resolveArgument(methodParameter, null, null, null);

        // then
        assertNotNull(resolvedUser);
        assertEquals(user, resolvedUser);
    }

    @Test
    @DisplayName("@AuthSeller를 사용하면 Role.SELLER를 가진 User를 반환한다")
    void resolveAuthSeller() throws Exception {
        // given
        User seller = User.builder()
            .id(2L)
            .uuid("uuid-seller")
            .nickname("sellerNickname")
            .role(Role.SELLER)
            .createdAt(null)
            .updatedAt(null)
            .deletedAt(null)
            .build();

        mockAuthentication(seller);
        MethodParameter methodParameter = mockMethodParameter(AuthSeller.class);

        // when
        Object resolvedSeller = authenticationResolver.resolveArgument(methodParameter, null, null, null);

        // then
        assertNotNull(resolvedSeller);
        assertEquals(seller, resolvedSeller);
    }

    @Test
    @DisplayName("@AuthUser를 사용했지만 Role이 SELLER이면 null을 반환한다")
    void authUserWithWrongRole() throws Exception {
        // given
        User seller = User.builder()
            .id(2L)
            .uuid("uuid-seller")
            .nickname("sellerNickname")
            .role(Role.SELLER)
            .createdAt(null)
            .updatedAt(null)
            .deletedAt(null)
            .build();
        mockAuthentication(seller);
        MethodParameter methodParameter = mockMethodParameter(AuthUser.class);

        // when
        Object resolvedUser = authenticationResolver.resolveArgument(methodParameter, null, null, null);

        // then
        assertNull(resolvedUser);
    }

    @Test
    @DisplayName("@AuthSeller를 사용했지만 Role이 USER이면 null을 반환한다")
    void authSellerWithWrongRole() throws Exception {
        // given
        User seller = User.builder()
            .id(2L)
            .uuid("uuid-seller")
            .nickname("sellerNickname")
            .role(Role.USER)
            .createdAt(null)
            .updatedAt(null)
            .deletedAt(null)
            .build();
        mockAuthentication(seller);
        MethodParameter methodParameter = mockMethodParameter(AuthSeller.class);

        // when
        Object resolvedSeller = authenticationResolver.resolveArgument(methodParameter, null, null, null);

        // then
        assertNull(resolvedSeller);
    }

    @Test
    @DisplayName("인증 정보가 없는 경우 null을 반환한다")
    void noAuthenticationReturnsNull() throws Exception {
        // given
        SecurityContextHolder.clearContext();
        MethodParameter methodParameter = mockMethodParameter(AuthUser.class);

        // when
        Object resolvedUser = authenticationResolver.resolveArgument(methodParameter, null, null, null);

        // then
        assertNull(resolvedUser);
    }

    @Test
    @DisplayName("Principal이 User가 아닌 경우 null을 반환한다")
    void invalidPrincipalReturnsNull() throws Exception {
        // given
        mockAuthentication("Invalid Principal");
        MethodParameter methodParameter = mockMethodParameter(AuthUser.class);

        // when
        Object resolvedUser = authenticationResolver.resolveArgument(methodParameter, null, null, null);

        // then
        assertNull(resolvedUser);
    }

    /***
     * SecurityContextHolder에 가짜 Authentication 객체 설정
     */
    private void mockAuthentication(Object principal) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * @AuthUser 또는 @AuthSeller를 가지는 MethodParameter 생성
     */
    private MethodParameter mockMethodParameter(Class<? extends Annotation> annotation) {
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.hasParameterAnnotation(annotation)).thenReturn(true);
        return methodParameter;
    }
}
