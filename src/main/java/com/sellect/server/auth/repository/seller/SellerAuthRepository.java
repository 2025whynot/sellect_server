package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.domain.SellerAuth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public interface SellerAuthRepository {
    void save(SellerAuth sellerAuth);

    SellerAuth findByEmail(String email);

    boolean existByEmail(@Email(message = "유효한 이메일 형식이 아닙니다.") @NotBlank(message = "이메일은 필수 입력 사항입니다.") String email);
}
