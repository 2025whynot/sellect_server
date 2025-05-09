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
import com.sellect.server.order.repository.fake.FakeOrderItemRepository;
import com.sellect.server.order.repository.fake.FakeOrdersRepository;
import com.sellect.server.payment.event.message.OrderReadyMessage;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.FakeInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    private ValueOperations<String, String> valueOperations; // redisTemplate.opsForValue()가 반환할 Mock
    @Mock
    private RedisTransactionUtil redisTransactionUtil;
    @Mock
    private KafkaProducer kafkaProducer;

    private User testUser;
    private Orders testOrder;
    private Orders testOrderCompleted;
    private Product testProduct1;
    private Product testProduct2;
    private Inventory testInventory1;
    private Inventory testInventory2;
    private OrderItem testOrderItem1;
    private OrderItem testOrderItem2;
    private Coupon testCoupon;
    private UserReceivedCoupon testUserReceivedCoupon;

    @BeforeEach
    void setUp() {

        // Fake Repository 초기화
        userRepository.clear();
        orderRepository.clear();
        orderItemRepository.clear();
        inventoryRepository.clear();
        userReceivedCouponRepository.clear();

        sut = new OrderServiceV4(
                userRepository, orderRepository, orderItemRepository,
                inventoryRepository, userReceivedCouponRepository,
                redisTemplate, redisTransactionUtil, kafkaProducer
        );

//        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // RedisTransactionUtil Mocking: transaction 메서드가 Consumer를 실행하도록 설정
//        doAnswer(invocation -> {
//            Consumer<RedisOperations<String, String>> consumer = invocation.getArgument(0);
//            consumer.accept(redisTemplate); // 실제로는 redisTemplate의 mock을 전달해야함. 여기서는 redisTemplate 자체가 mock
//            return null;
//        }).when(redisTransactionUtil).transaction(any());

        // 기본 테스트 데이터 생성
        testUser = User.builder().id(1L).nickname("testUser").build();
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

        testInventory1 = Inventory.builder().id(201L).product(testProduct1).stock(10).build();
        testInventory2 = Inventory.builder().id(202L).product(testProduct2).stock(5).build();
        inventoryRepository.save(testInventory1);
        inventoryRepository.save(testInventory2);

        // 주문1: product1 2개 (pending) -> 총 금액 = 150.00 * 2 = 300.00
        testOrder = Orders.builder()
                .id(1L)
                .user(testUser)
                .userReceivedCoupon(null) // 쿠폰 없음
                .totalPrice(new BigDecimal("300.00"))
                .status(OrderStatus.PENDING) // 초기 상태 PENDING
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleteAt(null)
                .build();
        orderRepository.save(testOrder);

        // 주문2: product2 3개 (completed) -> 총 금액 = 100.00 * 3 = 300.00
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

        // product1 2개 -> 150.00 * 2 = 300.00
        testOrderItem1 = OrderItem.builder()
                .id(301L)
                .orders(testOrder) // 주문1 (pending)
                .productId(testProduct1.getId())
                .quantity(2)
                .price(new BigDecimal("300.00"))
                .build();
        // product2 3개 주문 -> 100.00 * 3 = 300.00
        testOrderItem2 = OrderItem.builder()
                .id(302L)
                .orders(testOrder) // 주문2 (completed)
                .productId(testProduct2.getId())
                .quantity(3)
                .price(new BigDecimal("300.00"))
                .build();
        orderItemRepository.saveAll(List.of(testOrderItem1, testOrderItem2));

        // 할인 금액 = 10
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

    @Nested
    @DisplayName("prepareOrder 테스트")
    class PrepareOrderTests {

        @Test
        @DisplayName("성공: 쿠폰 없이 주문 준비")
        void prepareOrder_NoCoupon_Success() {
            // When
            sut.prepareOrder(testUser.getId(), testOrder.getId(), null);

            // Then
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
            // Given
            BigDecimal originalPrice = testOrder.getTotalPrice();
            BigDecimal discountedPrice = originalPrice.subtract(new BigDecimal(testUserReceivedCoupon.getCoupon().getDiscountCost()));

            // When
            sut.prepareOrder(testUser.getId(), testOrder.getId(), testUserReceivedCoupon.getId());

            // Then
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
            // When & Then
            assertThatThrownBy(() -> sut.prepareOrder(999L, testOrder.getId(), null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("user"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 주문")
        void prepareOrder_OrderNotFound_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> sut.prepareOrder(testUser.getId(), 999L, null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("order"));
        }

        @Test
        @DisplayName("실패: 주문 소유자 불일치")
        void prepareOrder_OrderOwnerMismatch_ThrowsException() {
            // Given
            User anotherUser = User.builder().id(2L).nickname("anotherUser").build();
            userRepository.save(anotherUser); // 다른 사용자 저장

            // When & Then
            assertThatThrownBy(() -> sut.prepareOrder(anotherUser.getId(), testOrder.getId(), null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.ACCESS_DENIED.getMessage("order"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 쿠폰")
        void prepareOrder_CouponNotFound_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> sut.prepareOrder(testUser.getId(), testOrder.getId(), 999L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("coupon"));
        }

        @Test
        @DisplayName("실패: 쿠폰 적용 시 이미 사용된 쿠폰인 경우")
        void prepareOrder_WithCoupon_CouponAlreadyUsed_ThrowsException() {
            // Given
            userReceivedCouponRepository.save(testUserReceivedCoupon.useCoupon());

            // When & Then
            assertThatThrownBy(() -> sut.prepareOrder(testUser.getId(), testOrder.getId(), testUserReceivedCoupon.getId()))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.COUPON_ALREADY_USED.getMessage(String.valueOf(testUserReceivedCoupon.getId())));
        }
    }

}