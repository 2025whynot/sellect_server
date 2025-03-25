package com.sellect.server.order.repository;

import com.sellect.server.auth.domain.User;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.domain.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository {

    Orders save(Orders orders);

    Optional<Orders> findById(Long id);

    Optional<Orders> findByIdAndStatus(Long id, OrderStatus status);

    List<Orders> findCompletedOrdersByUser(User user, OrderStatus status);

    // todo: 일단은 DB를 수정하기 보다는 비관적인 락을 통한 테스트를 진행
    Optional<Orders> findByIdWithPessimisticLock(Long id);


    // todo: 그러고 성능 테스트 후에 -> DB를 수정해서 낙관적인 락을 사용하는 걸로 방향 잡음
}
