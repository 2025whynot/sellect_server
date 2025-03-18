package com.sellect.server.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.FakeUserRepository;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.FakeBrandRepository;
import com.sellect.server.cart.domain.CartItem;
import com.sellect.server.cart.repository.FakeCartItemRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.FakeuserReceivedCouponRepository;
import com.sellect.server.order.controller.request.OrderAddRequest;
import com.sellect.server.order.controller.request.OrderItemAddRequest;
import com.sellect.server.order.controller.response.OrderDetailGetResponse;
import com.sellect.server.order.controller.response.OrderGetResponse;
import com.sellect.server.order.controller.response.PendingOrderRegisterResponse;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.order.repository.fake.FakeOrderItemRepository;
import com.sellect.server.order.repository.fake.FakeOrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.KakaoPayReadyEvent;
import com.sellect.server.payment.repository.FakePaymentRepository;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import com.sellect.server.product.repository.FakeInventoryRepository;
import com.sellect.server.product.repository.FakeProductImageRepository;
import com.sellect.server.product.repository.FakeProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class) // Mockito 환경 자동 초기화
class OrderServiceTest {

    private final FakeOrdersRepository ordersRepository = new FakeOrdersRepository();
    private final FakeOrderItemRepository orderItemRepository = new FakeOrderItemRepository();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final FakeInventoryRepository inventoryRepository = new FakeInventoryRepository();
    private final FakeCartItemRepository cartRepository = new FakeCartItemRepository();
    private final FakeuserReceivedCouponRepository userReceivedCouponRepository = new FakeuserReceivedCouponRepository();
    private final FakeProductImageRepository productImageRepository = new FakeProductImageRepository();
    private final FakeBrandRepository brandRepository = new FakeBrandRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final FakePaymentRepository paymentRepository = new FakePaymentRepository();
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private OrderService sut; // sut을 BeforeEach에서 초기화
    private User user;

    @BeforeEach
    void setUp() {
        ordersRepository.clear();
        orderItemRepository.clear();
        productRepository.clear();
        inventoryRepository.clear();
        cartRepository.clear();
        userReceivedCouponRepository.clear();
        brandRepository.clear();
        productImageRepository.clear();
        userRepository.clear();
        user = User.builder()
            .id(1L)
//            .uuid("userUuid")
            .build();
        sut = new OrderService(
            ordersRepository,
            orderItemRepository,
            productRepository,
            inventoryRepository,
            cartRepository,
            userReceivedCouponRepository,
            productImageRepository,
            userRepository,
            paymentRepository,
            eventPublisher
        );
    }

    @Nested
    @DisplayName("주문 등록 테스트")
    class RegisterPendingOrderTest {

        @Test
        @DisplayName("주문, 주문 아이템 생성 성공")
        void testRegisterPendingOrder() {
            // Given
            Product product1 = productRepository.save(Product.builder()
                .id(1L)
                .build());

            Product product2 = productRepository.save(Product.builder()
                .id(2L)
                .build());

            inventoryRepository.save(Inventory.builder()
                .id(1L)
                .product(product1)
                .stock(10)
                .build());

            inventoryRepository.save(Inventory.builder()
                .id(2L)
                .product(product2)
                .stock(10)
                .build());

            OrderAddRequest request = new OrderAddRequest(
                "200000",
                List.of(
                    new OrderItemAddRequest(1L, "10000", 5),
                    new OrderItemAddRequest(2L, "20000", 5)
                )
            );

            // When
            PendingOrderRegisterResponse response = sut.registerPendingOrder(user, request);

            // Then
            Orders savedOrder = ordersRepository.findById(response.orderId()).orElseThrow();
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(savedOrder.getId());
            assertThat(orderItems).hasSize(2);
        }

        @Test
        @DisplayName("재고 부족 시, 에러 발생")
        void testStockError() {
            // Given
            Product product = productRepository.save(Product.builder()
                .id(1L)
                .build());

            inventoryRepository.save(Inventory.builder()
                .id(1L)
                .product(product)
                .stock(5)
                .build());

            OrderAddRequest request = new OrderAddRequest(
                "200000",
                List.of(
                    new OrderItemAddRequest(1L, "10000", 10)
                )
            );

            // When
            CommonException exception = assertThrows(CommonException.class, () ->
                sut.registerPendingOrder(user, request));
            // Then
            assertEquals("재고 부족 is not valid", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("결제 요청 테스트")
    class PayOrderTest {

        @Test
        @DisplayName("쿠폰 사용하지 않고 주문 업데이트")
        void testPayOrderWithoutCoupon() {
            // Given
            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("50000"))
                .build());

            String expectedUrl = "mocked-url";
            doAnswer(invocation -> {
                KakaoPayReadyEvent event = invocation.getArgument(0);
                event.getFuture().complete(expectedUrl);
                return null;
            }).when(eventPublisher).publishEvent(any(KakaoPayReadyEvent.class));

            // When
            sut.payOrder(user, order.getId(), null);

            // Then
            Orders updatedOrder = ordersRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(updatedOrder.getUserReceivedCoupon()).isNull();
        }

        @Test
        @DisplayName("쿠폰 적용 후 주문 업데이트")
        void testPayOrderWithCoupon() {
            // Given
            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("50000"))
                .build());

            Coupon coupon = Coupon.builder()
                .id(1L)
                .discountCost(10000)
                .build();

            userReceivedCouponRepository.save(UserReceivedCoupon.builder()
                .id(1L)
                .user(user)
                .coupon(coupon)
                .isUsed(false)
                .build());

            String expectedUrl = "mocked-url";
            doAnswer(invocation -> {
                KakaoPayReadyEvent event = invocation.getArgument(0);
                event.getFuture().complete(expectedUrl);
                return null;
            }).when(eventPublisher).publishEvent(any(KakaoPayReadyEvent.class));

            // When
            sut.payOrder(user, order.getId(), 1L);

            // Then
            Orders updatedOrder = ordersRepository.findById(order.getId()).orElseThrow();
            assertEquals(1L, updatedOrder.getUserReceivedCoupon().getId());
            assertEquals(new BigDecimal("40000"), updatedOrder.getTotalPrice());
            assertEquals(OrderStatus.PENDING, updatedOrder.getStatus());
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰으로 요청 시 예외 발생")
        void testPayOrderWithInvalidCoupon() {
            // Given
            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build());

            // When & Then
            CommonException exception = assertThrows(CommonException.class,
                () -> sut.payOrder(user, order.getId(), 999L));
            assertEquals("쿠폰 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("주문 소유자가 아닌 경우 예외 발생")
        void testPayOrderInvalidOwner() {
            // Given
            User anotherUser = User.builder()
                .id(2L)
                .build();
            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(anotherUser)
                .status(OrderStatus.PENDING)
                .build());

            // When & Then
            assertThrows(CommonException.class,
                () -> sut.payOrder(user, order.getId(), null));
        }
    }

    @Nested
    @DisplayName("결제 승인 테스트")
    class ApprovePaymentTest {

        @Test
        @DisplayName("재고 차감 및 주문 완료 상태 변경")
        void testApprovePaymentOrderCompletion() {
            // Given
            userRepository.save(user); // UUID를 위해 저장

            Product product = productRepository.save(Product.builder()
                .id(1L)
                .build());

            Inventory inventory = inventoryRepository.save(Inventory.builder()
                .id(1L)
                .product(product)
                .stock(10)
                .build());

            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("50000"))
                .build());

            orderItemRepository.saveAll(List.of(
                OrderItem.builder()
                    .id(1L)
                    .orders(order)
                    .product(product)
                    .quantity(5)
                    .build()));

            Payment payment = Payment.builder()
                .pid("pid123")
                .orderId(order.getId().toString())
//                .uid(user.getUuid())
                .userId(user.getId())
                .build();

            paymentRepository.save(payment);

            // When
            sut.approvePayment("pid123", "token123");

            // Then
            Orders updatedOrder = ordersRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

            Inventory updatedInventory = inventoryRepository.findById(inventory.getId())
                .orElseThrow();
            assertThat(updatedInventory.getStock()).isEqualTo(5); // 재고 차감 확인
        }

        @Test
        @DisplayName("재고 부족 시 예외 발생")
        void testApprovePaymentStockInsufficient() {
            // Given
            userRepository.save(user); // UUID를 위해 저장

            Product product = productRepository.save(Product.builder()
                .id(1L)
                .build());

            Inventory inventory = inventoryRepository.save(Inventory.builder()
                .id(1L)
                .product(product)
                .stock(5)
                .build());

            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build());

            orderItemRepository.saveAll(List.of(
                OrderItem.builder()
                    .id(1L)
                    .orders(order)
                    .product(product)
                    .quantity(10)
                    .build()));

            Payment payment = Payment.builder()
                .pid("pid123")
                .orderId(order.getId().toString())
//                .uid(user.getUuid())
                .userId(user.getId())
                .build();
            paymentRepository.save(payment);

            // When & Then
            CommonException exception = assertThrows(CommonException.class,
                () -> sut.approvePayment("pid123", "token123"));
            assertEquals("재고 부족 is not valid", exception.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 사용자일 경우 예외 발생")
        void testApprovePaymentInvalidUser() {
            // Given
            Orders order = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build());

            Payment payment = Payment.builder()
                .pid("pid123")
                .orderId(order.getId().toString())
//                .uid("invalid-uuid")
                .userId(user.getId())
                .build();

            paymentRepository.save(payment);

            // When & Then
            CommonException exception = assertThrows(CommonException.class,
                () -> sut.approvePayment("pid123", "token123"));
            assertEquals("user does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("장바구니 비우기 & 쿠폰 사용 처리 성공")
        void testClearCartCoupon() {
            // Given
            cartRepository.save(CartItem.builder()
                .id(1L)
                .user(user)
                .build());

            cartRepository.save(CartItem.builder()
                .id(2L)
                .user(user)
                .build());

            userReceivedCouponRepository.save(UserReceivedCoupon.builder()
                .id(1L)
                .user(user)
                .isUsed(false)
                .build());
            UserReceivedCoupon userCoupon = userReceivedCouponRepository.findById(1L).orElseThrow();

            Orders savedOrder = ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .userReceivedCoupon(userCoupon)
                .status(OrderStatus.COMPLETED)
                .build());

            // When
            sut.clearCartAndDeleteCoupon(user, savedOrder);

            // Then
            CartItem cart1 = cartRepository.findById(1L).orElseThrow();
            CartItem cart2 = cartRepository.findById(2L).orElseThrow();
            assertThat(cart1.getDeleteAt()).isNotNull();
            assertThat(cart2.getDeleteAt()).isNotNull();

            UserReceivedCoupon updatedCoupon = userReceivedCouponRepository.findById(1L)
                .orElseThrow();
            assertThat(updatedCoupon.getIsUsed()).isTrue();
        }
    }

    @Nested
    @DisplayName("주문 조회 테스트")
    class GetOrdersTest {

        Coupon coupon = Coupon.builder()
            .id(1L)
            .discountCost(10000)
            .build();
        UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.builder()
            .id(1L)
            .coupon(coupon)
            .build();

        @Test
        @DisplayName("사용자의 모든 주문을 조회 성공")
        void testGetOrdersByUser() {
            // Given
            ordersRepository.save(Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.COMPLETED)
                .build());
            ordersRepository.save(Orders.builder()
                .id(2L)
                .user(user)
                .status(OrderStatus.COMPLETED)
                .build());
            ordersRepository.save(Orders.builder()
                .id(3L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build());

            // When
            List<OrderGetResponse> orderResponses = sut.getOrdersByUser(user);

            // Then
            assertThat(orderResponses.size()).isEqualTo(2); // 확정 주문만 조회
        }

        @Test
        @DisplayName("주문 상세 정보 조회 성공")
        void testGetOrderDetail() {
            // Given
            Brand brand = brandRepository.save(Brand.builder()
                .id(1L)
                .name("브랜드1")
                .build());

            Product product1 = productRepository.save(Product.builder()
                .id(1L)
                .name("상품1")
                .brand(brand)
                .build());

            productImageRepository.save(ProductImage.builder()
                .id(1L)
                .sequence(1)
                .product(product1)
                .representative(true)
                .imageUrl("image1.url")
                .build(), product1);

            Orders savedOrder = ordersRepository.save(Orders.builder()
                .id(1L)
                .status(OrderStatus.COMPLETED)
                .totalPrice(new BigDecimal("50000"))
                .build());

            orderItemRepository.saveAll(List.of(
                OrderItem.builder()
                    .id(1L)
                    .orders(savedOrder)
                    .product(product1)
                    .build()
            ));

            // When
            OrderDetailGetResponse orderDetail = sut.getOrderDetail(savedOrder.getId());

            // Then
            assertThat(orderDetail.orderItems()).hasSize(1);
            assertThat(orderDetail.orderItems().get(0).productName()).isEqualTo("상품1");
            assertThat(orderDetail.totalPrice()).isEqualTo(new BigDecimal("50000"));
            assertEquals("브랜드1", orderDetail.orderItems().get(0).brandName());
            assertEquals("image1.url", orderDetail.orderItems().get(0).imageUrl());
        }
    }
}