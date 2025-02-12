package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.domain.Seller;
import com.sellect.server.auth.domain.SellerAuth;
import com.sellect.server.auth.repository.entity.SellerAuthEntity;
import com.sellect.server.auth.repository.entity.SellerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SellerAuthRepositoryImpl implements SellerAuthRepository {
    private final SellerAuthJpaRepository sellerAuthJpaRepository;

    @Override
    public void save(SellerAuth sellerAuth) {
        Seller seller = sellerAuth.getSeller();
        // todo: throw exception
        SellerEntity sellerEntity = SellerEntity.from(seller);
        SellerAuthEntity sellerAuthEntity = SellerAuthEntity.from(sellerEntity, sellerAuth);
        sellerAuthJpaRepository.save(sellerAuthEntity);
    }

    @Override
    public SellerAuth findByEmail(String email) {
        SellerAuthEntity sellerAuth = sellerAuthJpaRepository.findByEmail(email).orElseThrow();
        return sellerAuth.toModel();
    }
}
