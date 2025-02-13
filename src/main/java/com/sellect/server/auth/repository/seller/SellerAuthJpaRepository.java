package com.sellect.server.auth.repository.seller;

import com.sellect.server.auth.repository.entity.SellerAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerAuthJpaRepository extends JpaRepository<SellerAuthEntity, Long> {
    Optional<SellerAuthEntity> findByEmail(String email);
    boolean existsByEmailAndDeleteAtIsNull(String email);
}
