package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.domain.Seller;
import com.sellect.server.auth.repository.entity.SellerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SellerRepositoryImpl implements SellerRepository {
    private final SellerJpaRepository sellerJpaRepository;

    // todo
    @Override
    public Seller findByUuid(String uuid) {
        return Seller.builder().build();
    }

    @Override
    public Seller save(Seller seller) {
        SellerEntity sellerEntity = SellerEntity.from(seller);
        SellerEntity result = sellerJpaRepository.save(sellerEntity);
        return result.toModel();
    }

    @Override
    public Seller findById(Long id) {
        // todo: 이메일이 존재하지 않는 커스텀 exception
        SellerEntity sellerEntity = sellerJpaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("이메일 또는 패스워드가 존재하지 않습니다."));
        return sellerEntity.toModel();
    }
}
