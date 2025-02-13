package com.sellect.server.auth.repository;

import com.sellect.server.auth.domain.SellerAuth;
import com.sellect.server.auth.repository.seller.SellerAuthRepository;
import java.util.HashMap;
import java.util.Map;

public class FakeSellerAuthRepository implements SellerAuthRepository {

    Map<Long, SellerAuth> table = new HashMap<>();
    Long sequence = 1L;

    @Override
    public void save(SellerAuth sellerAuth) {
        if (sellerAuth.getId() == null) {
            sellerAuth = SellerAuth.builder().id(sequence++).email(sellerAuth.getEmail())
                .seller(sellerAuth.getSeller()).password(sellerAuth.getPassword())
                .createdAt(sellerAuth.getCreatedAt()).updatedAt(sellerAuth.getUpdatedAt())
                .deleteAt(sellerAuth.getDeleteAt()).build();
        }
        table.put(sellerAuth.getId(), sellerAuth);
    }

    @Override
    public SellerAuth findByEmail(String email) {
        return table.values().stream().filter(seller -> seller.getEmail().equals(email)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("이메일이 존재하지 않습니다."));
    }

    @Override
    public boolean existByEmail(String email) {
        return table.values().stream().anyMatch(seller -> seller.getEmail().equals(email));
    }
}
