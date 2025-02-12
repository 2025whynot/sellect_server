package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.domain.Seller;

public interface SellerRepository {
    Seller findByUuid(String uuid);
    Seller save(Seller seller);

    Seller findById(Long id);
}
