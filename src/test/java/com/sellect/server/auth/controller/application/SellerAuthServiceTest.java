package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.SellerAuth;
import com.sellect.server.auth.repository.FakeSellerAuthRepository;
import com.sellect.server.auth.repository.FakeSellerRepository;
import com.sellect.server.auth.repository.seller.SellerAuthRepository;
import com.sellect.server.auth.repository.seller.SellerRepository;
import com.sellect.server.common.infrastructure.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SellerAuthServiceTest {
    private SellerAuthService sellerAuthService;
    private SellerRepository sellerRepository;
    private SellerAuthRepository sellerAuthRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("TEST_SECRET_KEY_TEMP_UPPER_THAN_256_BIT");
        sellerRepository = new FakeSellerRepository();
        sellerAuthRepository = new FakeSellerAuthRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        sellerAuthService = new SellerAuthService(jwtUtil, sellerRepository, sellerAuthRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("Seller 회원가입 테스트")
    class SellerSignUpTest {

        @Test
        @DisplayName("성공적으로 판매자를 등록한다")
        void willSuccess() {
            // given
            UserSignUpRequest request = new UserSignUpRequest("seller@shop.com", "password123", "ShopOwner");

            // when
            sellerAuthService.signUp(request);

            // then
            SellerAuth savedSellerAuth = sellerAuthRepository.findByEmail(request.email());
            assertNotNull(savedSellerAuth);
            assertEquals(request.email(), savedSellerAuth.getEmail());
        }

        @Test
        @DisplayName("패스워드는 암호화한다.")
        void encryptPassword() {
            // given
            UserSignUpRequest request = new UserSignUpRequest("seller@shop.com", "password123", "ShopOwner");

            // when
            sellerAuthService.signUp(request);

            // then
            SellerAuth savedSellerAuth = sellerAuthRepository.findByEmail(request.email());
            assertNotNull(savedSellerAuth);
            assertNotEquals(request.password(), savedSellerAuth.getPassword());
            assertTrue(passwordEncoder.matches(request.password(), savedSellerAuth.getPassword()));
        }

        @Test
        @DisplayName("이메일은 중복되면 안된다.")
        void canNotDuplicateEmail() {
            // given
            UserSignUpRequest request1 = new UserSignUpRequest("seller@shop.com", "password123", "ShopOwner");
            UserSignUpRequest request2 = new UserSignUpRequest("seller@shop.com", "password456", "AnotherShop");
            sellerAuthService.signUp(request1);

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                sellerAuthService.signUp(request2);
            });

            assertEquals("Email already exists", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Seller 로그인 테스트")
    class SellerLoginTest {

        @Test
        @DisplayName("성공적으로 로그인하고 JWT 토큰을 발급받는다")
        void willGenerateJwtToken() {
            // given
            UserSignUpRequest signUpRequest = new UserSignUpRequest("seller@shop.com", "password123", "ShopOwner");
            sellerAuthService.signUp(signUpRequest);
            LoginRequest loginRequest = new LoginRequest("seller@shop.com", "password123");

            // when
            String token = sellerAuthService.login(loginRequest);

            // then
            assertNotNull(token);
            assertTrue(jwtUtil.isTokenValid(token));
        }

        @Test
        @DisplayName("비밀번호가 틀리면 로그인에 실패한다")
        void willFailWithWrongPassword() {
            // given
            UserSignUpRequest signUpRequest = new UserSignUpRequest("seller@shop.com", "password123", "ShopOwner");
            sellerAuthService.signUp(signUpRequest);
            LoginRequest wrongLoginRequest = new LoginRequest("seller@shop.com", "wrongpassword");

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                sellerAuthService.login(wrongLoginRequest);
            });

            assertEquals("비밀번호가 일치하지 않습니다.", thrown.getMessage());
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 로그인에 실패한다")
        void willFailWithNonExistingEmail() {
            // given
            LoginRequest nonExistingEmailRequest = new LoginRequest("nonexistent@shop.com", "password123");

            // when & then
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                sellerAuthService.login(nonExistingEmailRequest);
            });

            assertEquals("이메일이 존재하지 않습니다.", thrown.getMessage());
        }
    }
}
