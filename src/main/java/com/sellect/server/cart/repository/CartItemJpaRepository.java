package com.sellect.server.cart.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, Long> {

    @Query("SELECT c FROM CartItemEntity c "
        + "WHERE c.user.id = :userId "
        + "AND c.deleteAt IS NULL")
    List<CartItemEntity> findByUserId(Long userId);

}
