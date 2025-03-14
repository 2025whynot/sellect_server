package com.sellect.server.order.application.v1;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.order.controller.request.OrderAddRequest;
import com.sellect.server.order.controller.response.OrderDetailGetResponse;
import com.sellect.server.order.controller.response.OrderGetResponse;
import com.sellect.server.order.controller.response.OrderItemGetResponse;
import com.sellect.server.order.controller.response.PendingOrderRegisterResponse;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.KakaoPayApproveEvent;
import com.sellect.server.payment.event.KakaoPayReadyEvent;
import com.sellect.server.payment.repository.PaymentRepository;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.InventoryRepository;
import com.sellect.server.product.repository.ProductImageRepository;
import com.sellect.server.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV1After {

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        // todo: 일단 결제 관련은 PASS
        // todo: 추후 검토 예정
        // ------------------------------- [변경사항 - 결제 요청을 이벤트 발생 (1/2)] -------------------------------
        CompletableFuture<String> future = new CompletableFuture<>();
        KakaoPayReadyEvent kakaoPayReadyEvent = new KakaoPayReadyEvent(this, user, orderId, order,
            future);
        eventPublisher.publishEvent(kakaoPayReadyEvent);
        String nextRedirectPcUrl = null;
        try {
            nextRedirectPcUrl = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        // 결제 요청
        return nextRedirectPcUrl;
        // ------------------------------- [변경사항 - 결제 요청을 이벤트 발생 (2/2)] -------------------------------
    }

    @Transactional
    public void approvePayment(String pid, String token) {

        Payment payment = paymentRepository.findByPid(pid)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.format("Payment %s", pid)));

        Long orderId = Long.valueOf(payment.getOrderId());

        // todo: (UUID 검색 - 성능 이슈 고려 필요)
        // todo: pid를 컬럼에서 pk (payment_id)로 통일함에 따라 굳이 uuid로 userRepository 찾을 필요없이 paymentRepository를 찾는다.
        // todo: 바꿔야함 paymentRepository.findByidAndUuid() - 2번째 발표 이후
        User user = userRepository.findByUuid(payment.getUid())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));

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

        inventoryRepository.saveAll(deductedInventories);
        // ------------------------------- [재고 동시성 방지  - (2/2)] -------------------------------

        ordersRepository.save(order.changeStatus(OrderStatus.COMPLETED)); // AFTER[1]
        // ------------------------------- [중복 결제 방지 - (3/3)] -------------------------------

        // todo: 해당 부분은 동시성이 괜찮을까?
        // todo: 주문에 대한 락이 잡혀있는 지금 상황에서 쿠폰 사용에 대한 동시성은 불필요할까?
        // todo: 이는 직접 테스트를 통해 확인해보고 싶음.
        // 해당 코드를 보면 getUserReceivedCoupon() 이는 추가적인 쿼리를 발생시킨다.
        // 읽는 작업과
        if (order.getUserReceivedCoupon() != null) {
            // 쓰기 작업이 분리되어있음. <- 원자성 보장이 안되어있음.
            userReceivedCouponRepository.save(order.getUserReceivedCoupon().useCoupon());
        }

        // todo : 결제 서비스에 요청 - 해당 부분 일단 PASS
        KakaoPayApproveEvent event = KakaoPayApproveEvent.publish(payment, token, pid);
        eventPublisher.publishEvent(event);
    }

    // ----------------------[밑에 로직들은 핵심이 아니기에 우선순위에 배제] ---------------------------

    /**
     * 주문 페이지 조회용 (결제 전)
     */
    @Transactional(readOnly = true)
    public List<OrderItemGetResponse> readPending(User user, Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

        order.validateOwner(user);
        order.validatePending();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        if (orderItems.isEmpty()) {
            throw new CommonException(BError.NOT_EXIST, "주문 아이템");
        }

        return orderItems.stream()
            .map(this::convertToOrderItemResponse)
            .toList();
    }

    /**
     * 주문 생성(pending)
     */
    @Transactional
    public PendingOrderRegisterResponse registerPendingOrder(User user, OrderAddRequest request) {

        // 일단 PASS
        Orders order = Orders.register(user, request.convertPriceAsBigDecimal(),
            OrderStatus.PENDING);
        Orders savedOrder = ordersRepository.save(order);

        Set<Long> productIds = new HashSet<>();
        List<OrderItem> orderItems = request.orderItems().stream()
            .map(orderItemAddRequest -> {
                // 같은 상품이 다른 orderItem 에 중복 등록되는 경우 방지
                if (!productIds.add(orderItemAddRequest.productId())) {
                    throw new CommonException(BError.EXIST, "productId");
                }
                // 상품이 존재하는지 확인
                Product product = productRepository.findById(orderItemAddRequest.productId())
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));

                // 상품의 재고를 파악해서 현재 품질인지만 확인
                Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(
                        () -> new CommonException(BError.NOT_EXIST, "inventory"));

                // 현재 수량 기준으로 주문이 가능하더라도
                // 어짜피 결제에서 재고 동시성 한번 하기에 최소한 불필요한 주문생성을 막기 위한 로직인듯.
                inventory.validateStock(orderItemAddRequest.quantity());
                return OrderItem.register(
                    savedOrder,
                    product,
                    orderItemAddRequest.convertPriceAsBigDecimal(),
                    orderItemAddRequest.quantity()
                );
            })
            .toList();

        orderItemRepository.saveAll(orderItems);

        // todo: 단순 orderId만 반환 -> Long 으로 수정
        // 결국은 orderId를 반환하는 것이기에 해당 부분 수정할 예정
        // PendingOrderRegisterResponse에 다른 무언가가 있을 것처럼 보이기에 수정할 예정
        return PendingOrderRegisterResponse.builder() // Controller 에서 from()으로 처리할 것
            .orderId(savedOrder.getId())
            .build();
    }

    /**
     * 주문 내역 확인
     */
    // 주문 내역 확인이기에 리스트 조회
    // 디테일하게 가져가면 페이지네이션 적용 필요 (하지만 우선순위에서 배제)
    @Transactional(readOnly = true)
    public List<OrderGetResponse> getOrdersByUser(User user) {
        List<Orders> orderList = ordersRepository.findCompletedOrdersByUser(user,
            OrderStatus.COMPLETED);

        // 주문 목록이 비어있다면? 빈 리스트를 반환하는게 일반적이지 않나? 일단, PASS
        if (orderList.isEmpty()) {
            throw new CommonException(BError.NOT_EXIST, "주문 목록");
        }

        return orderList.stream()
            .map(order -> {
                List<OrderItemGetResponse> orderItems = orderItemRepository.findAllByOrdersId(
                        order.getId())
                    .stream()
                    .map(this::convertToOrderItemResponse)
                    .toList();
                return OrderGetResponse.from(order, orderItems);
            })
            .toList();
    }

    /**
     * 주문 내역 상세 확인
     */
    public OrderDetailGetResponse getOrderDetail(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

        // OOP... 하지만 넘긴다.
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new CommonException(BError.NOT_VALID, "PENDING 상태의 주문은 조회할 수 없습니다.");
        }

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);

        if (orderItems.isEmpty()) {
            throw new CommonException(BError.NOT_EXIST, "주문 아이템");
        }

        List<OrderItemGetResponse> orderItemsResponse = orderItems.stream()
            .map(this::convertToOrderItemResponse)
            .toList();

        BigDecimal discountCost = Optional.ofNullable(order.getUserReceivedCoupon())
            .map(UserReceivedCoupon::getCoupon)
            .map(Coupon::getDiscountCost)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);

        return OrderDetailGetResponse.from(order, discountCost, orderItemsResponse);
    }

    private OrderItemGetResponse convertToOrderItemResponse(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        String thumbnailImageUrl = productImageRepository.findByThumbnailImage(product.getId())
            .getImageUrl();
        return OrderItemGetResponse.from(orderItem, product, thumbnailImageUrl);
    }
}