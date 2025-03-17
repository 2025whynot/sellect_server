package com.sellect.server.auth.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.FakeUserAuthRepository;
import com.sellect.server.auth.repository.FakeUserRepository;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private UserRepository userRepository;
    private UserAuthRepository userAuthRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        userAuthRepository = new FakeUserAuthRepository();
        userService = new UserService(userRepository, userAuthRepository);
    }

    @Nested
    @DisplayName("유저 삭제 테스트")
    class UserDeleteTest {
        @Test
        @DisplayName("유저를 삭제한다")
        void willDeleteUser() {
            // given
            User user = User.builder()
                .id(1L)
//                .uuid("test-uuid")
                .nickname("test-nickname")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();

            UserAuth userAuth = UserAuth.builder()
                .email("test@test.com")
                .password("test_password")
                .user(user)
                .build();
            userRepository.save(user);
            userAuthRepository.save(userAuth);
            // when
            userService.leave(user);

            Optional<User> byId = userRepository.findById(1L);
            User deletedUser = byId.get();

            // then
            assertNotNull(deletedUser.getDeleteAt());
        }
    }
}