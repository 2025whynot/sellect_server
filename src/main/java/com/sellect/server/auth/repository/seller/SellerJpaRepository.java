package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.repository.entity.SellerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerJpaRepository extends JpaRepository<SellerEntity, Long> {
}
