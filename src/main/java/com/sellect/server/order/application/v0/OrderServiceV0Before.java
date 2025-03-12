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
public class OrderServiceV0Before {

    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final PaymentServiceV0 paymentService;
    private final UserReceivedCouponRepository userReceivedCouponRepository;


    // 주문 결제 전 준비 (결제 금액을 카카오 서버에 보내기 위해)
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


    // todo: 여기에도 궁금한데 한번 걸어보자. 지금 현재 findReadyPaymentByPid로 찾아와서 결제 완료가 되는데 이게 잘 되는건가?
    @Transactional
    public void approvePayment(String pid, String token) {
        // 확인한다.
        // todo: 넘겨도 될듯
        // todo: 성능 이슈 생길 가능성 있음 (random UUID 타입 미지정) 인덱스 시 애매함..
        Payment payment = paymentService.findReadyPaymentByPid(pid);
        try {
            // order
            // start
            Long orderId = Long.valueOf(payment.getOrderId());
            // todo: 여기 개느릴듯 uuid로 찾기에
            User user = userRepository.findByUuid(payment.getUid())
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));

            // todo: ------------------------------- 중복 결제 방지 (1) -------------------------------
            // todo: findByIdWithLock
            Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

            // 이미 완료된 주문인지 확인
            // todo: 여기 동시성 안 터지나? 결제 pid를 통해 찾고 이를 기반으로 COMPLETED 하기에 괜찮은건가? <- 성능 테스트를 통해 확인해볼 것
            // todo: 중복 결제를 미리 여기서 막아야 하는거 아닌가?
            // todo: 중복 결제는 자주 일어나는 일일까? <- 낙관적 락 (OPTIMISTIC) 사용을 일단 해보자!
            if (order.getStatus() == OrderStatus.COMPLETED) {
                throw new CommonException(BError.NOT_VALID, "이미 완료(확정)된 주문입니다.");
            }
            // todo: ------------------------------- 중복 결제 방지 (1) -------------------------------

            // todo: 애초에 주문이 생성 안되어있어야 하는거 아님? (필수인가?) <- 그렇다고 굳이 지울 필요는 없음
            List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
            if (orderItems.isEmpty()) {
                throw new CommonException(BError.NOT_EXIST, "주문 아이템");
            }

            // todo: ------------------------------- 재고 동시성 방지 (1) -------------------------------
            List<Inventory> deductedInventories = orderItems.stream()
                .map(orderItem -> {
                    Product product = orderItem.getProduct();
                    // DB 락
                    // todo: 여기서 락을 잡는구나
                    // todo : @Lock(LockModeType.PESSIMISTIC_READ) <- 근데 재고 수정인데 PESSIMISTIC_WRITE를 해야 안전하지 않을까?
                    Inventory inventory = inventoryRepository.findWithLockByProductId(
                            product.getId())
                        .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "inventory"));
                    // 재고 확인 및 차감
                    // todo: 이해 안되는 부분 - 이거 그러면 inventory의 수량을 변경한다는거고 그래서 락을 잡았다?!
                    return orderItem.deductStock(inventory);
                })
                .toList();
            // todo: 락을 잡고 save를 같이 해야 원자성이 보장되는걸로 알고 있는데 <- 동시성 고려가 적합하게 된건가?

            // todo: before
            deductedInventories.forEach(inventoryRepository::save);
            // todo: after
            // todo: ------------------------------- 재고 동시성 방지 (2) -------------------------------

            // todo: 여기는 뭐 그럴 수 있고
            // todo : 로직으로 뺄 것
            ordersRepository.save(order.changeStatus(OrderStatus.COMPLETED));
            // todo: ------------------------------- 중복 결제 방지 (1) -------------------------------
            // 쿠폰 사용 처리, 장바구니 비우기
            // clearCartAndDeleteCouponAsync(user, savedOrder);

            // todo : 결제 서비스에 요청
            // todo: 일단은 결제 승인 전에 로직 구현을 검증
            // paymentService.paymentApprove(pid, token, payment);
        } catch (Exception e) {
            log.info("OrderService - [Duplicated] Failed to approve payment for pid: {}", pid);
            // todo:롤백 안됨
        }
    }
}
