package com.sellect.server.order.repository.fake;

import com.sellect.server.auth.domain.User;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FakeOrdersRepository implements OrdersRepository {

    private final List<Orders> data = new ArrayList<>();
    private long idSequence = 1L;

    // --- 예외 발생 제어를 위한 필드 추가 ---
    private Long triggerSaveFailureForOrderId = null;
    private OrderStatus triggerSaveFailureForOrderStatus = null;
    private RuntimeException exceptionToThrowOnSave = null;

    /**
     * 다음에 save 메소드가 특정 조건의 Orders 객체와 함께 호출될 때 지정된 예외를 발생시키도록 설정
     * @param orderId 대상 주문 ID
     * @param status  대상 주문 상태 (이 상태의 주문이 저장되려고 할 때 예외 발생)
     * @param ex      발생시킬 RuntimeException
     */
    public void setNextSaveToFail(Long orderId, OrderStatus status, RuntimeException ex) {
        this.triggerSaveFailureForOrderId = orderId;
        this.triggerSaveFailureForOrderStatus = status;
        this.exceptionToThrowOnSave = ex;
    }

    @Override
    public Orders save(Orders orderInput) {
        // --- 예외 발생 조건 체크 로직 추가 ---
        if (triggerSaveFailureForOrderId != null &&
                Objects.equals(triggerSaveFailureForOrderId, orderInput.getId()) &&
                (triggerSaveFailureForOrderStatus == null || triggerSaveFailureForOrderStatus == orderInput.getStatus()) &&
                exceptionToThrowOnSave != null) {

            RuntimeException exToThrow = this.exceptionToThrowOnSave;
            // 다음 save 호출에 영향을 주지 않도록 조건 초기화
            this.triggerSaveFailureForOrderId = null;
            this.triggerSaveFailureForOrderStatus = null;
            this.exceptionToThrowOnSave = null;
            throw exToThrow;
        }

        Orders orderToSave;

        if (orderInput.getId() == null) {
            // 새로 생성되는 주문
            orderToSave = Orders.builder()
                    .id(idSequence++)
                    .user(orderInput.getUser())
                    .userReceivedCoupon(orderInput.getUserReceivedCoupon())
                    .totalPrice(orderInput.getTotalPrice())
                    .orderNumber(orderInput.getOrderNumber()) // 주문 번호 복사
                    .status(orderInput.getStatus())
                    .createdAt(orderInput.getCreatedAt() != null ? orderInput.getCreatedAt() : LocalDateTime.now()) // 생성 시간 설정
                    .updatedAt(LocalDateTime.now()) // 업데이트 시간도 현재로 설정
                    .deleteAt(orderInput.getDeleteAt())
                    .build();
        } else {
            // 기존 주문 업데이트 (예: 상태 변경)
            // OrderService의 completeOrder는 새로운 Orders 객체를 만들어 전달하므로,
            // 전달된 order 객체의 필드를 기반으로 저장/업데이트해야 함.
            orderToSave = Orders.builder()
                    .id(orderInput.getId()) // 기존 ID 사용
                    .user(orderInput.getUser())
                    .userReceivedCoupon(orderInput.getUserReceivedCoupon())
                    .totalPrice(orderInput.getTotalPrice())
                    .orderNumber(orderInput.getOrderNumber())
                    .status(orderInput.getStatus())
                    .createdAt(orderInput.getCreatedAt()) // 기존 생성 시간 유지
                    .updatedAt(LocalDateTime.now())   // 업데이트 시간은 현재로
                    .deleteAt(orderInput.getDeleteAt())
                    .build();
        }
        data.removeIf(existingOrder -> existingOrder.getId().equals(orderToSave.getId()));
        data.add(orderToSave);
        return orderToSave;
    }

    @Override
    public Optional<Orders> findById(Long id) {
        return data.stream()
                .filter(order -> order.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<Orders> findByIdAndStatus(Long id, OrderStatus status) {
        // 구현: ID와 상태 모두 일치하는 주문 검색
        return data.stream()
                .filter(order -> order.getId().equals(id) && order.getStatus() == status)
                .findFirst();
    }

    @Override
    public List<Orders> findCompletedOrdersByUser(User user, OrderStatus status) {
        return data.stream()
                .filter(order -> order.getUser().getId().equals(user.getId())
                        && order.getStatus() == status)
                .toList();
    }

    @Override
    public Optional<Orders> findByIdWithPessimisticLock(Long id) {
        // Fake Repository에서는 비관적 락의 특별한 동작을 시뮬레이션하기 어려우므로 findById와 동일하게 동작
        return findById(id);
    }

    public void clear() {
        data.clear();
        idSequence = 1L; // ID 시퀀스도 초기화
        // 예외 발생 조건도 초기화
        this.triggerSaveFailureForOrderId = null;
        this.triggerSaveFailureForOrderStatus = null;
        this.exceptionToThrowOnSave = null;
    }
}