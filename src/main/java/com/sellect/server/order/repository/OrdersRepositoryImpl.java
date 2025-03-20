package com.sellect.server.order.repository;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.order.repository.entity.OrdersEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrdersRepositoryImpl implements OrdersRepository {

    private final OrdersJpaRepository ordersJpaRepository;

    @Override
    public Orders save(Orders orders) {
        return ordersJpaRepository.save(OrdersEntity.from(orders)).toModel();
    }

    // todo: PENDING 인지 체크
    @Override
    public Optional<Orders> findById(Long id) {
        return ordersJpaRepository.findById(id).map(OrdersEntity::toModel);
    }

    @Override
    public List<Orders> findCompletedOrdersByUser(User user, OrderStatus status) {
        List<OrdersEntity> ordersEntities = ordersJpaRepository.findCompletedOrdersByUser(
            UserEntity.from(user), status);

        return ordersEntities.stream().map(OrdersEntity::toModel).toList();
    }

    @Override
    public Optional<Orders> findByIdWithPessimisticLock(Long id) {
        return ordersJpaRepository.findByIdWithPessimisticLock(id).map(OrdersEntity::toModel);
    }
}
