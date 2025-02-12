package com.sellect.server.auth.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SellerAuth {
    private final Long id;
    private final Seller seller;
    private final String email;
    private final String password;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static SellerAuth signUp(Seller seller, String email, String password) {
        return SellerAuth.builder()
                .seller(seller)
                .email(email)
                .password(password)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();
    }
}
