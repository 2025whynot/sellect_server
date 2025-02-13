package com.sellect.server.auth.repository;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import java.util.HashMap;
import java.util.Map;

public class FakeUserRepository implements UserRepository {

    private final Map<Long, User> userStore = new HashMap<>();
    private long sequence = 1L;

    @Override
    public User findByUuid(String uuid) {
        return userStore.values().stream()
            .filter(user -> user.getUuid().equals(uuid))
            .findFirst()
            .orElse(null);
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user = User.builder()
                .id(sequence++)
                .uuid(user.getUuid())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
        }
        userStore.put(user.getId(), user);
        return user;
    }

    @Override
    public User findById(Long id) {
        return userStore.get(id);
    }
}

