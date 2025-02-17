package com.sellect.server.auth.repository.entity;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.BaseTimeEntity;
import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "`user`") // user는 예약어이므로 백틱 사용
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class UserEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String uuid;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    public static UserEntity from(User user) {
        return UserEntity.builder()
            .id(user.getId())
            .uuid(user.getUuid())
            .nickname(user.getNickname())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .deleteAt(user.getDeleteAt())
            .build();
    }

    public User toModel() {
        return User.builder()
            .id(this.id)
            .uuid(this.uuid)
            .nickname(this.nickname)
            .role(this.role)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }
}

