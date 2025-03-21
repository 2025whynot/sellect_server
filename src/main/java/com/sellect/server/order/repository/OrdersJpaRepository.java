package com.sellect.server.order.repository;

import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.order.repository.entity.OrdersEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdersJpaRepository extends JpaRepository<OrdersEntity, Long> {
    
    @Query("SELECT o FROM OrdersEntity o WHERE o.userEntity = :user AND o.status = :status ORDER BY o.updatedAt DESC")
    List<OrdersEntity> findCompletedOrdersByUser(UserEntity user,
        @Param("status") OrderStatus status);

    // v0 After 일단은 PESSIMISTIC_WRITE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrdersEntity o WHERE o.id = :id")
    Optional<OrdersEntity> findByIdWithPessimisticLock(@Param("id") Long id);

    // v1 After
    Optional<OrdersEntity> findByIdAndStatus(Long id, OrderStatus status);
}
