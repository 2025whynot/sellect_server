package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.repository.entity.SellerAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerAuthJpaRepository extends JpaRepository<SellerAuthEntity, Long> {
}
