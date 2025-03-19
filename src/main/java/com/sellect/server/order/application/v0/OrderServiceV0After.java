package com.sellect.server.order.application.v0;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.payment.application.PaymentServiceV0;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceV0After {

    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final PaymentServiceV0 paymentService;
    private final UserReceivedCouponRepository userReceivedCouponRepository;


    // 주문 결제
    @Transactional
    public String payOrder(User user, Long orderId, Long userReceivedCouponId) {

        // 주문 받아와서
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

        // 유저의 주문인지 확인
        order.validateOwner(user);

        // 쿠폰 적용
        if (userReceivedCouponId != null) {
            UserReceivedCoupon coupon = userReceivedCouponRepository.findById(userReceivedCouponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "쿠폰"));
            order = ordersRepository.save(order.applyCoupon(coupon));
        }

        // 결제 요청
        return paymentService.getKakaoPayReadyResponse(user, orderId, order);
    }


    @Transactional
    public void approvePayment(String pid, String token) {
        // Question 1
        // todo: 사실 이 부분부터 낙관적 락을 고려하는게 맞지 않을까. <- 그렇다고 하면 밑에 주문관련 락을 걸 필요가 사라짐
        Payment payment = paymentService.findReadyPaymentByPid(pid);
        try {
            Long orderId = Long.valueOf(payment.getOrderId());
            // todo: (UUID 검색 - 성능 이슈 고려 필요)
            // todo: pid를 컬럼에서 pk (payment_id)로 통일함에 따라 굳이 uuid로 userRepository 찾을 필요없이 paymentRepository를 찾는다.
            // 바꿔야함 paymentRepository.findByidAndUuid() - 2번째 발표 이후
//            userRepository.findByUuid(payment.getUid()).orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));
            userRepository.findById(payment.getUserId()).orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));

            // ------------------------------- [중복 결제 방지 - (1/3)] -------------------------------
            // 비관적 락 적용 (PESSIMISTIC_WRITE) - 동시에 같은 주문을 처리하지 못하도록
            Orders order = ordersRepository.findByIdWithPessimisticLock(orderId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

            // todo: 낙관 vs 비관 -> 추론: 낙관 (이유는 중복 결제가 현재 자주 발생하지 않을 것이라고 예상)
            if (order.getStatus() == OrderStatus.COMPLETED) {
                throw new CommonException(BError.NOT_VALID, "이미 완료(확정)된 주문입니다.");
            }
            // ------------------------------- [중복 결제 방지 - (2/3)] -------------------------------
            // todo: 일단 패스
            List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
            if (orderItems.isEmpty()) {
                throw new CommonException(BError.NOT_EXIST, "주문 아이템");
            }

            // ------------------------------- [재고 동시성 방지  - (1/2)] -------------------------------
            List<Inventory> deductedInventories = orderItems.stream()
                .map(orderItem -> {
                    Product product = orderItem.getProduct();
                    // DB 락
                    Inventory inventory = inventoryRepository.findWithWriteLockByProductId(
                            product.getId())
                        .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "inventory"));
                    // 재고 확인 및 차감
                    // todo: OOP를 다시 적용할 것! (일단 패스)
                    return orderItem.deductStock(inventory);
                })
                .toList();


            // deductedInventories.forEach(inventoryRepository::save); // BEFORE
            inventoryRepository.saveAll(deductedInventories); // AFTER
            // ------------------------------- [재고 동시성 방지  - (2/2)] -------------------------------

            ordersRepository.save(order.changeStatus(OrderStatus.COMPLETED));
            // ------------------------------- [중복 결제 방지 - (3/3)] -------------------------------

            // todo : 결제 서비스에 요청 - 해당 부분 일단 PASS
            // todo: 일단은 결제 승인 전에 로직 구현을 검증
             paymentService.paymentApprove(pid, token, payment);
        } catch (Exception e) {
//            log.error("Failed to approve payment for pid: {}", pid, e);
            log.info("OrderService - [Duplicated] Failed to approve payment for pid: {}", pid);
            // [1] 로그를 위한 에러 찍기 vs [2] 그냥 try-catch 없애기
            throw e;
        }
    }
}
