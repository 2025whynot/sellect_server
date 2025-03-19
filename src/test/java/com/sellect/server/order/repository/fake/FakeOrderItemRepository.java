package com.sellect.server.order.repository.fake;

import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeOrderItemRepository implements OrderItemRepository {

    private final List<OrderItem> data = new ArrayList<>();
    private long idSequence = 1L;

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        List<OrderItem> mutableOrderItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (item.getId() == null) {
                mutableOrderItems.add(OrderItem.builder()
                    .id(idSequence++)
                    .orders(item.getOrders())
                    .productId(item.getProductId())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .build());
            } else {
                mutableOrderItems.add(item);
            }
        }
        data.addAll(mutableOrderItems);
        return new ArrayList<>(mutableOrderItems);
    }

    @Override
    public List<OrderItem> findAllByOrdersId(Long orderId) {
        return data.stream()
            .filter(item -> item.getOrders().getId().equals(orderId))
            .toList();
    }

    @Override
    public Optional<BigDecimal> calculateSalesByProductId(Long productId) {
        BigDecimal totalSales = data.stream()
            .filter(item -> item.getProductId().equals(productId))
            .filter(item -> item.getOrders().getStatus()
                .equals(OrderStatus.COMPLETED)) // 주문 상태가 COMPLETED인 경우만
            .map(item -> item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))) // price * quantity
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(totalSales);
    }

    @Override
    public Optional<Integer> countCompleteOrdersByProductId(Long productId) {
        long uniqueOrderCount = data.stream()
            .filter(item -> item.getProductId().equals(productId))
            .filter(item -> item.getOrders().getStatus()
                .equals(OrderStatus.COMPLETED)) // 주문 상태가 COMPLETED인 경우만
            .map(item -> item.getOrders().getId())
            .distinct() // 중복 주문 제거
            .count();
        return Optional.of((int) uniqueOrderCount);
    }

    @Override
    public Optional<BigDecimal> calculateTotalSalesByProductIds(List<Long> productIds) {
        BigDecimal totalSales = data.stream()
            .filter(item -> productIds.contains(item.getProductId()))
            .filter(item -> item.getOrders().getStatus()
                .equals(OrderStatus.COMPLETED)) // 주문 상태가 COMPLETED인 경우만
            .map(item -> item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))) // price * quantity
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(totalSales);
    }

    public void clear() {
        data.clear();
        idSequence = 1L;
    }

    // 테스트용: 데이터 추가 메서드 (필요 시 사용)
    public void addOrderItem(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            OrderItem newItem = OrderItem.builder()
                .id(idSequence++)
                .orders(orderItem.getOrders())
                .productId(orderItem.getProductId())
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .build();
            data.add(newItem);
        } else {
            data.add(orderItem);
        }
    }
}