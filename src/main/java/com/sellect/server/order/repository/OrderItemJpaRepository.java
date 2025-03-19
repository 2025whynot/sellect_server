package com.sellect.server.order.repository;

import com.sellect.server.order.repository.entity.OrderItemEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findAllByOrdersEntityId(Long orderId);

    @Query("SELECT SUM(o.totalPrice) "
        + "FROM OrderItemEntity oi "
        + "JOIN oi.ordersEntity o "
        + "WHERE oi.productId = :productId "
        + "AND o.status = 'COMPLETED'")
    Optional<BigDecimal> calculateSalesByProductId(@Param("productId") Long productId);

    @Query("SELECT SUM(o.totalPrice) "
        + "FROM OrderItemEntity oi "
        + "JOIN oi.ordersEntity o "
        + "WHERE oi.productId IN :productIds "
        + "AND o.status = 'COMPLETED'")
    Optional<BigDecimal> calculateTotalSalesByProductIds(List<Long> productIds);

    @Query("SELECT COUNT(DISTINCT o.id) "
        + "FROM OrderItemEntity oi "
        + "JOIN oi.ordersEntity o "
        + "WHERE oi.productId = :productId "
        + "AND o.status = 'COMPLETED'")
    Optional<Integer> countCompletedOrdersByProductId(@Param("productId") Long productId);

}
