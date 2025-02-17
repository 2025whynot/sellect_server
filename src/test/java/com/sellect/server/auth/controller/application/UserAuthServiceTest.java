package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.FakeUserAuthRepository;
import com.sellect.server.auth.repository.FakeUserRepository;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.infrastructure.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class UserAuthServiceTest {

    private UserRepository userRepository;
    private UserAuthRepository userAuthRepository;
    private PasswordEncoder passwordEncoder;
    private UserAuthService userAuthService;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("TEST_SECRET_KEY_TEMP_UPPER_THAN_256_BIT", 1000L);
        userRepository = new FakeUserRepository();
        userAuthRepository = new FakeUserAuthRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        userAuthService = new UserAuthService(jwtUtil, userAuthRepository, userRepository,
            passwordEncoder);
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class UserSignUpRequestTest {

        @Test
        @DisplayName("성공적으로 USER를 등록한다")
        void willSuccessUser() {
            testSignUp(Role.USER, "user@user.com", "user123", "useruser");
        }

        @Test
        @DisplayName("성공적으로 SELLER를 등록한다")
        void willSuccessSeller() {
            testSignUp(Role.SELLER, "seller@seller.com", "seller123", "sellerseller");
        }

        private void testSignUp(Role role, String email, String password, String nickname) {
            // given
            UserSignUpRequest request = new UserSignUpRequest(email, password, nickname);

            // when
            userAuthService.signUp(request, role);

            // then
            UserAuth savedUserAuth = userAuthRepository.findByEmail(email).orElseThrow();
            assertNotNull(savedUserAuth);
            assertEquals(email, savedUserAuth.getEmail());
        }

        @Test
        @DisplayName("패스워드는 암호화된다.")
        void encryptPassword() {
            testPasswordEncryption(Role.USER, "user@user.com", "user123", "useruser");
            testPasswordEncryption(Role.SELLER, "seller@seller.com", "seller123", "sellerseller");
        }

        private void testPasswordEncryption(Role role, String email, String password,
            String nickname) {
            // given
            UserSignUpRequest request = new UserSignUpRequest(email, password, nickname);

            // when
            userAuthService.signUp(request, role);

            // then
            UserAuth savedUserAuth = userAuthRepository.findByEmail(email).orElseThrow();
            assertNotNull(savedUserAuth);
            assertNotEquals(password, savedUserAuth.getPassword());
            assertTrue(passwordEncoder.matches(password, savedUserAuth.getPassword()));
        }

        @Test
        @DisplayName("이메일은 중복될 수 없다")
        void cannotDuplicateEmail() {
            testDuplicateEmail(Role.USER, "duplicate@user.com", "user123", "useruser");
            testDuplicateEmail(Role.SELLER, "duplicate@seller.com", "seller123", "sellerseller");
        }

        private void testDuplicateEmail(Role role, String email, String password, String nickname) {
            // given
            UserSignUpRequest request1 = new UserSignUpRequest(email, password, nickname);
            UserSignUpRequest request2 = new UserSignUpRequest(email, "newPass", "newNick");
            userAuthService.signUp(request1, role);

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                userAuthService.signUp(request2, role);
            });

            assertEquals("Email already exists", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class UserLoginRequestTest {

        @Test
        @DisplayName("USER가 성공적으로 로그인한다")
        void willSuccessUser() {
            testLoginSuccess(Role.USER, "user@user.com", "user123", "useruser");
        }

        @Test
        @DisplayName("SELLER가 성공적으로 로그인한다")
        void willSuccessSeller() {
            testLoginSuccess(Role.SELLER, "seller@seller.com", "seller123", "sellerseller");
        }

        private void testLoginSuccess(Role role, String email, String password, String nickname) {
            // given
            UserSignUpRequest signUpRequest = new UserSignUpRequest(email, password, nickname);
            userAuthService.signUp(signUpRequest, role);

            LoginRequest loginRequest = new LoginRequest(email, password);

            // when
            String token = userAuthService.login(loginRequest, role);

            // then
            assertNotNull(token);
            assertTrue(jwtUtil.isTokenValid(token));
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 로그인 실패")
        void emailNotFound() {
            testLoginFailure(Role.USER, "unknown@user.com", "password123", "User not found");
            testLoginFailure(Role.SELLER, "unknown@seller.com", "password123", "User not found");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 로그인 실패")
        void wrongPassword() {
            // given
            UserSignUpRequest signUpRequest = new UserSignUpRequest("user@user.com", "user123",
                "correctpassword");
            userAuthService.signUp(signUpRequest, Role.USER);

            LoginRequest loginRequest = new LoginRequest("user@user.com", "wrongpassword");

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                userAuthService.login(loginRequest, Role.USER);
            });

            assertEquals("Invalid password", thrown.getMessage());
        }

        private void testLoginFailure(Role role, String email, String password,
            String expectedMessage) {
            // given
            LoginRequest loginRequest = new LoginRequest(email, password);

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                userAuthService.login(loginRequest, role);
            });

            assertEquals(expectedMessage, thrown.getMessage());
        }
    }
}
