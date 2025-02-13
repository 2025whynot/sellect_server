package com.sellect.server.auth.repository.entity;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class UserAuthEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    public static UserAuthEntity from (User user, UserAuth userAuth) {
        return UserAuthEntity.builder()
                .id(user.getId())
                .user(UserEntity.from(user))
                .email(userAuth.getEmail())
                .password(userAuth.getPassword())
                .createdAt(userAuth.getCreatedAt())
                .updatedAt(userAuth.getUpdatedAt())
                .deleteAt(userAuth.getDeleteAt())
                .build();
    }

    public UserAuth toModel() {
        return UserAuth.builder()
                .id(this.id)
                .user(this.user.toModel())
                .email(this.email)
                .password(this.password)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .deleteAt(this.getDeleteAt())
                .build();
    }
}
