package com.sellect.server.auth.domain;

import com.sellect.server.auth.repository.user.UserRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAuth {
    private final Long id;
    private final User user;
    private final String email;
    private final String password;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;


    public static UserAuth signUp(User user, String email, String password) {
        return UserAuth.builder()
                .user(user)
                .email(email)
                .password(password)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();
    }
}
