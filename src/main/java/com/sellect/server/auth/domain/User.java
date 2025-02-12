package com.sellect.server.auth.domain;

import com.sellect.server.auth.repository.entity.UserEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    private final Long id;
    private final String uuid;
    private final String nickname;  // 닉네임은 변경 가능하도록 설정
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    public static User register (String nickname) {
        return User.builder()
                .uuid(UUID.randomUUID().toString())
                .nickname(nickname)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", nickname='" + nickname + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}

