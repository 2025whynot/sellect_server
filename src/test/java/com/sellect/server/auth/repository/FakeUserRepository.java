package com.sellect.server.auth.repository;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeUserRepository implements UserRepository {

    private final Map<Long, User> userStore = new HashMap<>();
    private long sequence = 1L;

    @Override
    public Optional<User> findByUuid(String uuid) {
        User foundUser = userStore.values().stream()
            .filter(user -> user.getUuid().equals(uuid))
            .findFirst()
            .orElse(null);

        return Optional.ofNullable(foundUser);
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user = User.builder()
                .id(sequence++)
                .uuid(user.getUuid())
                .nickname(user.getNickname())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
        }
        userStore.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        User user = userStore.get(id);
        return Optional.ofNullable(user);
    }
}

