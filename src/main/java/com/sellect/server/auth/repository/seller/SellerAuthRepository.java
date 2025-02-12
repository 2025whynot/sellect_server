package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.domain.SellerAuth;

public interface SellerAuthRepository {
    void save(SellerAuth sellerAuth);

    SellerAuth findByEmail(String email);
}
