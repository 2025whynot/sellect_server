package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.FakeUserAuthRepository;
import com.sellect.server.auth.repository.FakeUserRepository;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.infrastructure.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UserAuthServiceTest {

    private UserRepository userRepository;
    private UserAuthRepository userAuthRepository;
    private PasswordEncoder passwordEncoder;
    private UserAuthService userAuthService;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("TEST_SECRET_KEY_TEMP_UPPER_THAN_256_BIT");
        userRepository = new FakeUserRepository();
        userAuthRepository = new FakeUserAuthRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        userAuthService = new UserAuthService(jwtUtil, userAuthRepository, userRepository,
            passwordEncoder);
    }


    @Nested
    @DisplayName("User 회원가입 테스트")
    class UserSignUpRequestTest {

        @Test
        @DisplayName("성공적으로 유저를 등록한다")
        void willSuccess() {
            UserSignUpRequest request = new UserSignUpRequest("user@user.com", "user123",
                "useruser");

            // when
            userAuthService.signUp(request);

            // then
            UserAuth savedUserAuth = userAuthRepository.findByEmail(request.email());
            assertNotNull(savedUserAuth);
            assertEquals(request.email(), savedUserAuth.getEmail());
        }

        @Test
        @DisplayName("패스워드는 암호화한다.")
        void encryptPassword() {
            // given
            UserSignUpRequest request = new UserSignUpRequest("user@user.com", "user123",
                "useruser");

            // when
            userAuthService.signUp(request);

            // then
            UserAuth savedUserAuth = userAuthRepository.findByEmail(request.email());
            assertNotNull(savedUserAuth);
            assertNotEquals(request.password(), savedUserAuth.getPassword());
            assertTrue(passwordEncoder.matches(request.password(), savedUserAuth.getPassword()));
        }

        @Test
        @DisplayName("이메일은 중복되면 안된다.")
        void canNotDuplicateEmail() {
            // given
            UserSignUpRequest request1 = new UserSignUpRequest("user@user.com", "user123",
                "useruser");
            UserSignUpRequest request2 = new UserSignUpRequest("user@user.com", "user456",
                "anotheruser");
            userAuthService.signUp(request1);

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                userAuthService.signUp(request2);
            });

            assertEquals("Email already exists", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("User 로그인 테스트")
    class UserLoginRequestTest {

        @Test
        @DisplayName("성공적으로 로그인할 수 있다")
        void willSuccess() {
            // given
            UserSignUpRequest signUpRequest = new UserSignUpRequest("user@user.com", "user123", "useruser");
            userAuthService.signUp(signUpRequest);

            LoginRequest loginRequest = new LoginRequest("user@user.com", "user123");

            // when
            String token = userAuthService.login(loginRequest);

            // then
            assertNotNull(token);
            assertTrue(jwtUtil.isTokenValid(token));
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 로그인 실패")
        void emailNotFound() {
            // given
            LoginRequest loginRequest = new LoginRequest("unknown@user.com", "password123");

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                userAuthService.login(loginRequest);
            });

            assertEquals("User not found", thrown.getMessage());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 로그인 실패")
        void wrongPassword() {
            // given
            UserSignUpRequest signUpRequest = new UserSignUpRequest("user@user.com", "user123", "correctpassword");
            userAuthService.signUp(signUpRequest);

            LoginRequest loginRequest = new LoginRequest("user@user.com", "wrongpassword");

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                userAuthService.login(loginRequest);
            });

            assertEquals("Invalid password", thrown.getMessage());
        }
    }


}