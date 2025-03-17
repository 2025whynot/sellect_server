package com.sellect.server.auth.domain;

import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private final Long id;
//    private final String uuid;
    private final String nickname;  // 닉네임은 변경 가능하도록 설정
    private final Role role;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static User register(String nickname, Role role) {
        return User.builder()
//            .uuid(UUID.randomUUID().toString())
            .nickname(nickname)
            .role(role)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }

    public User delete() {
        return User.builder()
            .id(this.id)
//            .uuid(this.uuid)
            .nickname(this.nickname)
            .role(this.role)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .deleteAt(LocalDateTime.now())
            .build();
    }

    // 역할 확인
    public void checkRole(Role role) {
        if (this.role != role) {
            if (role == Role.SELLER) {
                throw new CommonException(BError.NOT_SELLER, this.nickname);
            } else {
                throw new CommonException(BError.NOT_USER, this.nickname);
            }
        }
    }
}

