package com.sellect.server.order.repository;

import static org.assertj.core.api.BDDAssertions.then;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.auth.repository.user.UserJpaRepository;
import com.sellect.server.config.JpaConfig;
import com.sellect.server.config.JsonConfig;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.domain.OrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// JPA 관련 Bean만 로드
@DataJpaTest
// @Container와 @ServiceConnection을 사용해 MySQLContainer를 클래스 단위에서 실행
@Testcontainers
// Prevent auto-configuration of H2
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, JsonConfig.class})
class OrdersRepositoryImplTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test-db")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private OrdersJpaRepository ordersJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private OrdersRepositoryImpl ordersRepository;
    private UserEntity userEntity;
    private User user;

    @BeforeEach
    void setUp() {
        ordersRepository = new OrdersRepositoryImpl(ordersJpaRepository);

        // UserEntity 저장
        userEntity = UserEntity.builder()
//            .uuid("test-uuid")
            .nickname("Test User")
            .role(Role.USER)
            .build();
        userJpaRepository.save(userEntity);

        user = userEntity.toModel();
    }

    @Nested
    @DisplayName("save()")
    class OrderSaveTest {

        @Test
        @DisplayName("새로 생성된 order를 저장한다.")
        void saveNewOrder() {
            // given
            Orders order = Orders.register(user, new BigDecimal("100000"), OrderStatus.PENDING);

            // when
            Orders savedOrders = ordersRepository.save(order);

            // then
            then(savedOrders).isNotNull();
            then(savedOrders.getId()).isNotNull();
            then(savedOrders.getUser().getId()).isEqualTo(userEntity.getId());
            then(savedOrders.getUserReceivedCoupon()).isNull();
            then(savedOrders.getOrderNumber()).isNotNull();
            then(savedOrders.getTotalPrice()).isEqualTo(new BigDecimal("100000"));
            then(savedOrders.getStatus()).isEqualTo(OrderStatus.PENDING);
            then(savedOrders.getCreatedAt()).isNotNull();
            then(savedOrders.getUpdatedAt()).isNotNull();
            then(savedOrders.getCreatedAt()).isEqualTo(savedOrders.getUpdatedAt());
            then(savedOrders.getDeleteAt()).isNull();
        }
    }
}