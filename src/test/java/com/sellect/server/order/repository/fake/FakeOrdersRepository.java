package com.sellect.server.order.repository.fake;

import com.sellect.server.auth.domain.User;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.order.repository.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeOrdersRepository implements OrdersRepository {

    private final List<Orders> data = new ArrayList<>();
    private long idSequence = 1L;

    @Override
    public Orders save(Orders order) {
        if (order.getId() == null) {
            order = Orders.builder()
                .id(idSequence++)
                .user(order.getUser())
                .userReceivedCoupon(order.getUserReceivedCoupon())
                .totalPrice(order.getTotalPrice())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }
        findById(order.getId()).ifPresent(data::remove);
        data.add(order);
        return order;
    }

    @Override
    public Optional<Orders> findById(Long id) {
        return data.stream()
            .filter(order -> order.getId().equals(id))
            .findFirst();
    }

    // todo: 테스트 작성 시 구현
    @Override
    public Optional<Orders> findByIdAndStatus(Long id, OrderStatus status) {
        return Optional.empty();
    }

    @Override
    public List<Orders> findCompletedOrdersByUser(User user, OrderStatus status) {
        return data.stream()
            .filter(order -> order.getUser().getId().equals(user.getId())
                && order.getStatus() == status)
            .toList();
    }

    // todo : 테스트시 작성 필요
    @Override
    public Optional<Orders> findByIdWithPessimisticLock(Long id) {
        return Optional.empty();
    }

    public void clear() {
        data.clear();
    }
}
