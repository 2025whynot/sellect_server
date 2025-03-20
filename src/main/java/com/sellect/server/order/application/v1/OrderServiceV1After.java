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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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
    private final DataSourceTransactionManager transactionManager;

    // 주문 결제
    public String preparePayment(User user, Long orderId, Long userReceivedCouponId) {

        // 트랜잭션 정의 및 시작
        TransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(definition);
        Orders order;

        try {
            // 주문 받아와서
            // todo: PENDING 인 것에 대해서만 조회 : 왜냐하면 준비, 승인 보상트랜잭션에서 CANCEL 여러개 조회 가능
            order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

            // 유저의 주문인지 확인
            order.validateOwner(user);

            // 쿠폰 적용
            if (userReceivedCouponId != null) {
                UserReceivedCoupon coupon = userReceivedCouponRepository.findById(
                        userReceivedCouponId)
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "쿠폰"));
                order = ordersRepository.save(order.applyCoupon(coupon));
            }

            transactionManager.commit(status);
        } catch (CommonException e) {
            transactionManager.rollback(status);
            throw e;
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "preparePayment() - 결제 준비 중 오류 발생");
        }

        // 트랜잭션 커밋 후 이벤트 발행
        CompletableFuture<String> future = new CompletableFuture<>();
        KakaoPayReadyEvent kakaoPayReadyEvent = new KakaoPayReadyEvent(this, user, order, future);
        eventPublisher.publishEvent(kakaoPayReadyEvent);
        try {
            return future.get(3, TimeUnit.SECONDS); //여기서 톰캣 스레드가 대기 - 타임아웃 추가, 응답 : nextRedirectPcUrl [결제 요청 QR]
        } catch (InterruptedException e) { // 3초 이전에 톰캣 스레드 interrupt()
            Thread.currentThread().interrupt();
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "결제 준비 중 인터럽트 발생");
        } catch (ExecutionException e) { // 비동기 작업 예외
            Throwable cause = e.getCause();
            if (cause instanceof CommonException) {
                throw (CommonException) cause;
            }
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "결제 준비 중 오류: " + cause.getMessage());
        } catch (TimeoutException e) { // 톰캣 대기 타임 아웃 초과 (타임아웃 지정 시)
            throw new CommonException(BError.TIMEOUT, "결제 준비 시간이 초과되었습니다");
        }
    }

    public void approvePayment(final Long pid, final String token) {

        // 트랜잭션 정의 및 시작
        TransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(definition);

        Payment payment;
        Orders order;

        try {
            // todo:[한번 더 생각해보기] CANCEL 상태를 걸러서 조회해야하는가? pid를 통해서 조회하기에 문제는 없다.
            payment = paymentRepository.findByPid(pid)
                .orElseThrow(() -> new CommonException(
                    BError.NOT_EXIST, String.format("Payment %s", pid)));

            userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new CommonException(BError.NOT_VALID, "userId"));

            // TODO 쿠폰 동시성 해결을 위한 락 구현

            // todo: 추론: 낙관 (이유는 중복 결제가 현재 자주 발생하지 않을 것이라고 예상)
            order = ordersRepository.findByIdWithPessimisticLock(payment.getOrdersId())
                .orElseThrow(() -> new CommonException(BError.NOT_VALID, "orderId"));

            order.validateNotCompleted();

            List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());
            if (orderItems.isEmpty()) { // 서비스에 위임
                throw new CommonException(BError.NOT_VALID, "orderId");
            }

            List<Inventory> deductedInventories = orderItems.stream()
                .map(orderItem -> {
                    Inventory inventory = inventoryRepository.findWithWriteLockByProductId(
                            orderItem.getProductId()) // orderItem 의 Product 연관관계가 꼭 필요한가?
                        .orElseThrow(() -> new CommonException(BError.NOT_VALID, "productId"));
                    return inventory.deductStock(orderItem.getQuantity());
                })
                .toList();

            inventoryRepository.saveAll(deductedInventories);
            ordersRepository.save(order.completeOrder());

            // 트랜잭션 커밋
            transactionManager.commit(status);
        } catch (CommonException e) {
            // 예외 발생 시 롤백
            transactionManager.rollback(status);
            throw e;
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "approvePayment() - 결제 승인 중 오류 발생");
        }

        // 트랜잭션 커밋 후 이벤트 발행
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
        Long productId = orderItem.getProductId();
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "상품"));
        String thumbnailImageUrl = productImageRepository.findByThumbnailImage(productId)
            .getImageUrl();
        return OrderItemGetResponse.from(orderItem, product, thumbnailImageUrl);
    }
}