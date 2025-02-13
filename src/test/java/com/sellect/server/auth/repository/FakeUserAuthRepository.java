package com.sellect.server.auth.repository;

import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import java.util.HashMap;
import java.util.Map;

public class FakeUserAuthRepository implements UserAuthRepository {

    private final Map<Long, UserAuth> userAuthStore = new HashMap<>();
    private long sequence = 1L;

    @Override
    public UserAuth save(UserAuth userAuth) {
        if (userAuth.getId() == null) {
            userAuth = UserAuth.builder()
                .id(sequence++)
                .email(userAuth.getEmail())
                .user(userAuth.getUser())
                .password(userAuth.getPassword())
                .createdAt(userAuth.getCreatedAt())
                .updatedAt(userAuth.getUpdatedAt())
                .deleteAt(userAuth.getDeleteAt())
                .build();
        }
        UserAuth savedUserAuth = userAuthStore.put(userAuth.getId(), userAuth);

        return savedUserAuth;
    }

    @Override
    public UserAuth findByEmail(String email) {
        return userAuthStore.values().stream()
            .filter(userAuth -> userAuth.getEmail().equals(email))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userAuthStore.values().stream()
            .anyMatch(userAuth -> userAuth.getEmail().equals(email));
    }
}
