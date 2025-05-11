package com.sellect.server.order.application.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.FakeUserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.common.redis.RedisTransactionUtil;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.FakeuserReceivedCouponRepository;
import com.sellect.server.coupon.repository.entity.CouponStatus;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.OrderStatus;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.event.message.StockHistoryMessage;
import com.sellect.server.order.repository.fake.FakeOrderItemRepository;
import com.sellect.server.order.repository.fake.FakeOrdersRepository;
import com.sellect.server.payment.event.message.OrderReadyMessage;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.FakeInventoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceV4Test {

    // SUT
    private OrderServiceV4 sut;

    // Fakes
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final FakeOrdersRepository orderRepository = new FakeOrdersRepository();
    private final FakeOrderItemRepository orderItemRepository = new FakeOrderItemRepository();
    private final FakeInventoryRepository inventoryRepository = new FakeInventoryRepository();
    private final FakeuserReceivedCouponRepository userReceivedCouponRepository = new FakeuserReceivedCouponRepository();

    // Mocks
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedisTransactionUtil redisTransactionUtil;
    @Mock
    private KafkaProducer kafkaProducer;

    private User testUser;
    private Orders testOrder;
    private Orders testOrderCompleted;
    private Product testProduct1;
    private Product testProduct2;
    private Inventory dbInventoryP1;
    private Inventory dbInventoryP2;
    private OrderItem testOrderItem1;
    private OrderItem testOrderItem2;
    private Coupon testCoupon;
    private UserReceivedCoupon testUserReceivedCoupon;

    @BeforeEach
    void setUp() {
        userRepository.clear();
        orderRepository.clear();
        orderItemRepository.clear();
        inventoryRepository.clear();
        userReceivedCouponRepository.clear();

        sut = new OrderServiceV4(
                userRepository, orderRepository, orderItemRepository,
                userReceivedCouponRepository,
                redisTemplate, redisTransactionUtil, kafkaProducer
        );

        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();
        userRepository.save(testUser);

        testProduct1 = Product.builder()
                .id(101L)
                .name("Test Product 1")
                .price(new BigDecimal("150.00"))
                .build();
        testProduct2 = Product.builder()
                .id(102L)
                .name("Test Product 2")
                .price(new BigDecimal("100.00"))
                .build();

        dbInventoryP1 = Inventory.builder()
                .id(201L)
                .product(testProduct1)
                .stock(10)
                .build();
        dbInventoryP2 = Inventory.builder()
                .id(202L)
                .product(testProduct2)
                .stock(5)
                .build();
        inventoryRepository.save(dbInventoryP1);
        inventoryRepository.save(dbInventoryP2);

        testOrder = Orders.builder()
                .id(1L)
                .user(testUser)
                .userReceivedCoupon(null)
                .totalPrice(new BigDecimal("300.00"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();
        orderRepository.save(testOrder);

        testOrderCompleted = Orders.builder()
                .id(2L)
                .user(testUser)
                .totalPrice(new BigDecimal("300.00"))
                .status(OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();
        orderRepository.save(testOrderCompleted);

        testOrderItem1 = OrderItem.builder()
                .id(301L)
                .orders(testOrder)
                .productId(testProduct1.getId())
                .quantity(2)
                .price(new BigDecimal("300.00"))
                .build();
        testOrderItem2 = OrderItem.builder()
                .id(302L)
                .orders(testOrderCompleted)
                .productId(testProduct2.getId())
                .quantity(3)
                .price(new BigDecimal("300.00"))
                .build();
        orderItemRepository.saveAll(List.of(testOrderItem1, testOrderItem2));

        testCoupon = Coupon.builder()
                .id(401L)
                .discountCost(10)
                .quantity(5)
                .expirationDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .couponStatus(CouponStatus.IN_STOCK)
                .build();

        testUserReceivedCoupon = UserReceivedCoupon.builder()
                .id(501L)
                .user(testUser)
                .coupon(testCoupon)
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();
        userReceivedCouponRepository.save(testUserReceivedCoupon);
    }

    @AfterEach
    void tearDown() {
        reset(redisTemplate, valueOperations, redisTransactionUtil, kafkaProducer);
    }

    @Nested
    @DisplayName("prepareOrder 테스트")
    class PrepareOrderTests {
        @Test
        @DisplayName("성공: 쿠폰 없이 주문 준비")
        void prepareOrder_NoCoupon_Success() {
            sut.prepareOrder(testUser.getId(), testOrder.getId(), null);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<OrderReadyMessage> messageCaptor = ArgumentCaptor.forClass(OrderReadyMessage.class);
            verify(kafkaProducer, times(1)).produce(topicCaptor.capture(), messageCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo("order-ready");
            OrderReadyMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getOrderId()).isEqualTo(testOrder.getId());
            assertThat(sentMessage.getUserId()).isEqualTo(testUser.getId());
            assertThat(sentMessage.getTotalPrice()).isEqualTo(testOrder.getTotalPrice().intValue());
        }

        @Test
        @DisplayName("성공: 쿠폰 적용하여 주문 준비")
        void prepareOrder_WithCoupon_Success() {
            BigDecimal originalPrice = testOrder.getTotalPrice();
            BigDecimal discountedPrice = originalPrice.subtract(new BigDecimal(testUserReceivedCoupon.getCoupon().getDiscountCost()));

            sut.prepareOrder(testUser.getId(), testOrder.getId(), testUserReceivedCoupon.getId());

            Orders orderAppliedCoupon = orderRepository.findById(testOrder.getId()).get();
            assertThat(orderAppliedCoupon.getTotalPrice()).isEqualByComparingTo(discountedPrice);
            assertThat(orderAppliedCoupon.getUserReceivedCoupon().getId()).isEqualTo(testUserReceivedCoupon.getId());

            ArgumentCaptor<OrderReadyMessage> messageCaptor = ArgumentCaptor.forClass(OrderReadyMessage.class);
            verify(kafkaProducer).produce(eq("order-ready"), messageCaptor.capture());
            assertThat(messageCaptor.getValue().getTotalPrice()).isEqualTo(discountedPrice.intValue());
            assertThat(messageCaptor.getValue().getTotalPrice()).isNotEqualTo(originalPrice.intValue());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void prepareOrder_UserNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.prepareOrder(999L, testOrder.getId(), null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("user"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 주문")
        void prepareOrder_OrderNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.prepareOrder(testUser.getId(), 999L, null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("order"));
        }

        @Test
        @DisplayName("실패: 주문 소유자 불일치")
        void prepareOrder_OrderOwnerMismatch_ThrowsException() {
            User anotherUser = User.builder()
                    .id(2L)
                    .nickname("anotherUser")
                    .build();
            userRepository.save(anotherUser);

            assertThatThrownBy(() -> sut.prepareOrder(anotherUser.getId(), testOrder.getId(), null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.ACCESS_DENIED.getMessage("order"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 쿠폰")
        void prepareOrder_CouponNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.prepareOrder(testUser.getId(), testOrder.getId(), 999L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("coupon"));
        }

        @Test
        @DisplayName("실패: 쿠폰 적용 시 이미 사용된 쿠폰인 경우")
        void prepareOrder_WithCoupon_CouponAlreadyUsed_ThrowsException() {
            userReceivedCouponRepository.save(testUserReceivedCoupon.useCoupon());

            assertThatThrownBy(() -> sut.prepareOrder(testUser.getId(), testOrder.getId(), testUserReceivedCoupon.getId()))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.COUPON_ALREADY_USED.getMessage(String.valueOf(testUserReceivedCoupon.getId())));
        }
    }

    @Nested
    @DisplayName("completeOrder 테스트")
    class CompleteOrderTests {

        @BeforeEach
        void setUpCompleteOrder() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doAnswer(invocation -> {
                Consumer<RedisOperations<String, String>> consumer = invocation.getArgument(0);
                consumer.accept(redisTemplate);
                return null;
            }).when(redisTransactionUtil).transaction(any());

            String stockKeyP1 = "product:" + testProduct1.getId() + ":stock";
            when(valueOperations.get(stockKeyP1)).thenReturn("10");
        }

        @Test
        @DisplayName("성공: 주문 완료 처리 시 남은 재고 수량 감소")
        void completeOrder_Success_DecrementsStockQuantity() {
            sut.completeOrder(testOrder.getId());

            Orders completedOrder = orderRepository.findById(testOrder.getId()).get();
            assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

            String stockKeyP1 = "product:" + testProduct1.getId() + ":stock";
            verify(valueOperations, times(1)).get(stockKeyP1);
            verify(valueOperations, times(1)).decrement(stockKeyP1, testOrderItem1.getQuantity());

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<StockHistoryMessage> messageCaptor = ArgumentCaptor.forClass(StockHistoryMessage.class);
            verify(kafkaProducer, times(1)).produce(topicCaptor.capture(), messageCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo("stock-history");
            StockHistoryMessage stockMessage = messageCaptor.getValue();
            assertThat(stockMessage.getUserId()).isEqualTo(testUser.getId());
            assertThat(stockMessage.getType()).isEqualTo("DECREMENT");
            assertThat(stockMessage.getHistoryItems()).hasSize(1);
            assertThat(stockMessage.getHistoryItems().get(0).getProductId()).isEqualTo(testProduct1.getId());
            assertThat(stockMessage.getHistoryItems().get(0).getQuantity()).isEqualTo(testOrderItem1.getQuantity());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 주문으로 완료 시도")
        void completeOrder_OrderNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.completeOrder(999L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("order"));
        }

        @Test
        @DisplayName("실패: 이미 완료된 주문을 다시 완료 시도")
        void completeOrder_AlreadyCompletedOrder_ThrowsException() {
            assertThatThrownBy(() -> sut.completeOrder(testOrderCompleted.getId()))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.FAIL_FOR_REASON.getMessage("order validation", "order status is COMPLETED"));
        }

        @Test
        @DisplayName("실패: 남은 재고 부족으로 주문 완료 실패 (내부에서 처리 후 return)")
        void completeOrder_DecrementStockFails_InsufficientStock_ReturnsNotThrows() {
            String stockKeyP1 = "product:" + testProduct1.getId() + ":stock";
            when(valueOperations.get(stockKeyP1)).thenReturn("1"); // 요청 수량(2)보다 적음

            sut.completeOrder(testOrder.getId());

            Orders order = orderRepository.findById(testOrder.getId()).get();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(kafkaProducer, never()).produce(anyString(), any(StockHistoryMessage.class));
            verify(valueOperations, never()).decrement(anyString(), anyLong());
        }

        @Test
        @DisplayName("실패: Redis에 재고 정보가 없을 때 (0으로 간주되어 재고 부족 발생 가능, 내부 처리 후 return)")
        void completeOrder_NoStockInfoInRedis_ReturnsNotThrows() {
            String stockKeyP1 = "product:" + testProduct1.getId() + ":stock";
            when(valueOperations.get(stockKeyP1)).thenReturn(null);

            sut.completeOrder(testOrder.getId());

            Orders order = orderRepository.findById(testOrder.getId()).get();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(kafkaProducer, never()).produce(anyString(), any(StockHistoryMessage.class));
            verify(valueOperations, never()).decrement(anyString(), anyLong());
        }

        @Test
        @DisplayName("실패: DB 저장 실패로 주문 완료 상태 저장 실패 시")
        void completeOrder_SaveCompletedStatusFails_ThrowsException() {
            String stockKeyP1 = "product:" + testProduct1.getId() + ":stock";
            // valueOperations.get(stockKeyP1)는 @BeforeEach에서 "10"으로 설정됨 (충분한 재고)

            orderRepository.setNextSaveToFail(
                    testOrder.getId(),
                    OrderStatus.COMPLETED,
                    new RuntimeException("DB save failed!")
            );

            assertThatThrownBy(() -> sut.completeOrder(testOrder.getId()))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.FAIL_FOR_REASON.getMessage("complete order", "unexpected error"));

            ArgumentCaptor<StockHistoryMessage> stockMessageCaptor = ArgumentCaptor.forClass(StockHistoryMessage.class);
            verify(kafkaProducer, times(1)).produce(eq("stock-history"), stockMessageCaptor.capture());
            assertThat(stockMessageCaptor.getValue().getType()).isEqualTo("DECREMENT");

            verify(valueOperations, times(1)).get(stockKeyP1);
            verify(valueOperations, times(1)).decrement(stockKeyP1, testOrderItem1.getQuantity());

            Orders orderAfterFailedSave = orderRepository.findById(testOrder.getId()).get();
            assertThat(orderAfterFailedSave.getStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("processOrderCompletionFailure 테스트")
    class ProcessOrderCompletionFailureTests {

        @BeforeEach
        void setUpProcessOrderCompletionFailure() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doAnswer(invocation -> {
                Consumer<RedisOperations<String, String>> consumer = invocation.getArgument(0);
                consumer.accept(redisTemplate);
                return null;
            }).when(redisTransactionUtil).transaction(any());
        }

        @Test
        @DisplayName("성공: 주문 실패 처리 시 남은 재고 수량 증가")
        void processOrderCompletionFailure_Success_IncrementsStockQuantity() {
            sut.processOrderCompletionFailure(testOrderCompleted.getId());

            Orders failedCompletedOrder = orderRepository.findById(testOrderCompleted.getId()).get();
            // Orders 도메인에 setOrderStatusToFailedCompleted()가 FAILED_COMPLETED로 변경한다고 가정
            assertThat(failedCompletedOrder.getStatus()).isEqualTo(OrderStatus.FAILED_COMPLETED);

            String stockKeyP2 = "product:" + testProduct2.getId() + ":stock";
            verify(valueOperations, times(1)).increment(stockKeyP2, testOrderItem2.getQuantity());

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<StockHistoryMessage> messageCaptor = ArgumentCaptor.forClass(StockHistoryMessage.class);
            verify(kafkaProducer, times(1)).produce(topicCaptor.capture(), messageCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo("stock-history");
            StockHistoryMessage stockMessage = messageCaptor.getValue();
            assertThat(stockMessage.getUserId()).isEqualTo(testUser.getId());
            assertThat(stockMessage.getType()).isEqualTo("INCREMENT");
            assertThat(stockMessage.getHistoryItems()).hasSize(1);
            assertThat(stockMessage.getHistoryItems().get(0).getProductId()).isEqualTo(testProduct2.getId());
            assertThat(stockMessage.getHistoryItems().get(0).getQuantity()).isEqualTo(testOrderItem2.getQuantity());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 주문으로 실패 처리 시도")
        void processOrderCompletionFailure_OrderNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.processOrderCompletionFailure(999L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("order"));
        }

        @Test
        @DisplayName("실패: PENDING 상태 주문을 실패 처리 시도")
        void processOrderCompletionFailure_PendingOrder_ThrowsException() {
            assertThatThrownBy(() -> sut.processOrderCompletionFailure(testOrder.getId()))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.FAIL_FOR_REASON.getMessage("order validation", "order status is not COMPLETED"));
        }
    }
}