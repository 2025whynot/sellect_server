package com.sellect.server.order.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.cart.domain.CartItem;
import com.sellect.server.cart.repository.CartItemRepository;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartItemRepository cartRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public String payOrder(User user, Long orderId, Long userReceivedCouponId) {
        Orders order = getOrderById(orderId);
        order.validateOwner(user);

        // 쿠폰 적용
        if (userReceivedCouponId != null) {
            UserReceivedCoupon coupon = userReceivedCouponRepository.findById(userReceivedCouponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "쿠폰"));
            order = ordersRepository.save(order.applyCoupon(coupon));
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        KakaoPayReadyEvent kakaoPayReadyEvent = new KakaoPayReadyEvent(this, user, order,
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
    }

    //tx1
    @Transactional
    public void approvePayment(Long pid, String token) {
        Payment payment = paymentRepository.findByPid(pid)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "payment"));


//        User user = userRepository.findByUuid(payment.getUid()).orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));
        User user = userRepository.findById(payment.getUserId()).orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));

        Orders order = getOrderById(payment.getOrdersId());
        List<OrderItem> orderItems = getOrderItemsByOrderId(order.getId());

        List<Inventory> deductedInventories = orderItems.stream()
            .map(orderItem -> {
                // DB 락
                Inventory inventory = inventoryRepository.findWithLockByProductId(
                        orderItem.getProductId())
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "inventory"));
                // 재고 확인 및 차감
                return orderItem.deductStock(inventory);
            })
            .toList();
        deductedInventories.forEach(inventoryRepository::save);
        // 주문 완료
        Orders savedOrder = ordersRepository.save(order.completeOrder());
        // 장바구니 비우기 및 쿠폰 삭제
        clearCartAndDeleteCouponAsync(user, savedOrder);

        KakaoPayApproveEvent event = KakaoPayApproveEvent.publish(payment, token, pid);
        eventPublisher.publishEvent(event);
    }

    @Async
    public void clearCartAndDeleteCouponAsync(User user, Orders order) {
        clearCartAndDeleteCoupon(user, order);
    }

    @Transactional
    public void clearCartAndDeleteCoupon(User user, Orders order) {
        order.validateCompleted();

        List<CartItem> cartItems = cartRepository.findAllByUserId(user.getId());
        List<CartItem> removedCartItems = cartItems.stream()
            .map(CartItem::remove)
            .toList();
        cartRepository.saveAll(removedCartItems);

        if (order.getUserReceivedCoupon() != null) {
            userReceivedCouponRepository.save(order.getUserReceivedCoupon().useCoupon());
        }
    }

    public List<OrderItemGetResponse> readPending(User user, Long orderId) {
        Orders order = getOrderById(orderId);
        order.validateOwner(user);
        order.validatePending();

        List<OrderItem> orderItems = getOrderItemsByOrderId(orderId);
        return orderItems.stream()
            .map(this::convertToOrderItemResponse)
            .toList();
    }

    @Transactional
    public PendingOrderRegisterResponse registerPendingOrder(User user, OrderAddRequest request) {

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
                Product product = productRepository.findById(orderItemAddRequest.productId())
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));
                Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(
                        () -> new CommonException(BError.NOT_EXIST, "inventory"));
                // 재고 확인
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

        return PendingOrderRegisterResponse.builder()
            .orderId(savedOrder.getId())
            .build();
    }

    public List<OrderGetResponse> getOrdersByUser(User user) {
        List<Orders> orderList = ordersRepository.findCompletedOrdersByUser(user,
            OrderStatus.COMPLETED);
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

    public OrderDetailGetResponse getOrderDetail(Long orderId) {
        Orders order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new CommonException(BError.NOT_VALID, "PENDING 상태의 주문은 조회할 수 없습니다.");
        }

        List<OrderItemGetResponse> orderItems = getOrderItemsByOrderId(orderId).stream()
            .map(this::convertToOrderItemResponse)
            .toList();

        BigDecimal discountCost = Optional.ofNullable(order.getUserReceivedCoupon())
            .map(UserReceivedCoupon::getCoupon)
            .map(Coupon::getDiscountCost)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);

        return OrderDetailGetResponse.from(order, discountCost, orderItems);
    }

    private OrderItemGetResponse convertToOrderItemResponse(OrderItem orderItem) {
        Long productId = orderItem.getProductId();
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "상품"));
        String thumbnailImageUrl = productImageRepository.findByThumbnailImage(productId)
            .getImageUrl();
        return OrderItemGetResponse.from(orderItem, product, thumbnailImageUrl);
    }

    private List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        if (orderItems.isEmpty()) {
            throw new CommonException(BError.NOT_EXIST, "주문 아이템");
        }
        return orderItems;
    }

    private Orders getOrderById(Long orderId) {
        return ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));
    }
}