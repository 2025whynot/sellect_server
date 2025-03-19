package com.sellect.server.order.repository;

import static org.assertj.core.api.BDDAssertions.then;

import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.auth.repository.user.UserJpaRepository;
import com.sellect.server.brand.repository.BrandEntity;
import com.sellect.server.brand.repository.BrandJpaRepository;
import com.sellect.server.category.repository.CategoryEntity;
import com.sellect.server.category.repository.CategoryJpaRepository;
import com.sellect.server.config.JpaConfig;
import com.sellect.server.config.JsonConfig;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.order.repository.entity.OrdersEntity;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductEntity;
import com.sellect.server.product.repository.ProductJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, JsonConfig.class})
class OrderItemRepositoryImplTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test-db")
        .withUsername("test")
        .withPassword("test");
    

    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private OrdersJpaRepository ordersJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    private OrderItemRepositoryImpl orderItemRepository;

    private UserEntity userEntity;
    private OrdersEntity ordersEntity;
    private CategoryEntity categoryEntity;
    private BrandEntity brandEntity;
    private ProductEntity productEntity;
    private Orders orders;
    private Product product;

    @BeforeEach
    void setUp() {
        orderItemRepository = new OrderItemRepositoryImpl(orderItemJpaRepository);

        userEntity = UserEntity.builder()
//            .uuid("test-uuid-" + System.currentTimeMillis())
            .nickname("Test User")
            .role(Role.USER)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        userJpaRepository.save(userEntity);

        ordersEntity = OrdersEntity.builder()
            .userEntity(userEntity)
            .totalPrice(new BigDecimal("100000"))
            .orderNumber("ORDER-1")
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        ordersJpaRepository.save(ordersEntity);
        orders = ordersEntity.toModel();

        UserEntity sellerEntity = UserEntity.builder()
//            .uuid("test-uuid-" + System.currentTimeMillis())
            .nickname("Test Seller")
            .role(Role.SELLER)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        userJpaRepository.save(sellerEntity);

        categoryEntity = CategoryEntity.builder()
            .name("Test Category")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        categoryJpaRepository.save(categoryEntity);

        brandEntity = BrandEntity.builder()
            .name("Test Brand")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        brandJpaRepository.save(brandEntity);

        productEntity = ProductEntity.builder()
            .name("Test Product")
            .sellerEntity(sellerEntity)
            .categoryEntity(categoryEntity)
            .brandEntity(brandEntity)
            .price(new BigDecimal("50000"))
            .description("Test Description")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        productJpaRepository.save(productEntity);
        product = productEntity.toModel();
    }

    @Nested
    @DisplayName("saveAll()")
    class OrderItemSaveAllTest {

        @Test
        @DisplayName("새로운 orderItem 리스트를 저장한다.")
        void saveNewOrderItems() {
            // given
            OrderItem orderItem1 = OrderItem.register(orders, product, new BigDecimal("50000"), 1);
            OrderItem orderItem2 = OrderItem.register(orders, product, new BigDecimal("50000"), 1);

            // when
            List<OrderItem> savedOrderItems = orderItemRepository.saveAll(
                List.of(orderItem1, orderItem2));

            // then
            then(savedOrderItems).isNotNull();
            then(savedOrderItems).hasSize(2);
            then(savedOrderItems.get(0).getOrders().getId()).isEqualTo(ordersEntity.getId());
            then(savedOrderItems.get(0).getProductId()).isEqualTo(productEntity.getId());
            then(savedOrderItems.get(0).getPrice()).isEqualTo(new BigDecimal("50000"));
            then(savedOrderItems.get(0).getQuantity()).isEqualTo(1);
            then(savedOrderItems.get(1).getOrders().getId()).isEqualTo(ordersEntity.getId());
            then(savedOrderItems.get(1).getProductId()).isEqualTo(productEntity.getId());
            then(savedOrderItems.get(1).getPrice()).isEqualTo(new BigDecimal("50000"));
            then(savedOrderItems.get(1).getQuantity()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findAllByOrdersId()")
    class FindAllByOrdersIdTest {

        @Test
        @DisplayName("ordersId로 orderItem 리스트를 조회한다.")
        void willSuccess() {
            // given
            OrderItem orderItem1 = OrderItem.builder()
                .orders(orders)
                .productId(product.getId())
                .price(new BigDecimal("50000"))
                .quantity(1)
                .createdAt(LocalDateTime.now())
                .build();
            OrderItem orderItem2 = OrderItem.builder()
                .orders(orders)
                .productId(product.getId())
                .price(new BigDecimal("50000"))
                .quantity(1)
                .createdAt(LocalDateTime.now())
                .build();
            orderItemRepository.saveAll(List.of(orderItem1, orderItem2));

            // when
            List<OrderItem> foundOrderItems = orderItemRepository.findAllByOrdersId(
                ordersEntity.getId());

            // then
            then(foundOrderItems).isNotNull();
            then(foundOrderItems).hasSize(2);
            then(foundOrderItems.get(0).getOrders().getId()).isEqualTo(ordersEntity.getId());
//            then(foundOrderItems.get(0).getProduct().getId()).isEqualTo(productEntity.getId());
            then(foundOrderItems.get(0).getPrice()).isEqualTo(new BigDecimal("50000"));
            then(foundOrderItems.get(0).getQuantity()).isEqualTo(1);
            then(foundOrderItems.get(1).getOrders().getId()).isEqualTo(ordersEntity.getId());
//            then(foundOrderItems.get(1).getProduct().getId()).isEqualTo(productEntity.getId());
            then(foundOrderItems.get(1).getPrice()).isEqualTo(new BigDecimal("50000"));
            then(foundOrderItems.get(1).getQuantity()).isEqualTo(1);
        }
    }
}