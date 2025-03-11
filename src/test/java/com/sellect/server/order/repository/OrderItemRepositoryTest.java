//package com.sellect.server.order.repository;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import com.sellect.server.auth.repository.entity.Role;
//import com.sellect.server.auth.repository.entity.UserEntity;
//import com.sellect.server.order.domain.OrderItem;
//import com.sellect.server.order.repository.entity.OrderStatus;
//import com.sellect.server.order.repository.entity.OrdersEntity;
//import com.sellect.server.product.repository.ProductEntity;
//import jakarta.persistence.EntityManager;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@DataJpaTest
//@Testcontainers
//class OrderItemRepositoryTest {
//
//    @Autowired
//    EntityManager em;
//
//    @Autowired
//    private OrderItemJpaRepository orderItemJpaRepository;
//    private OrderItemRepositoryImpl orderItemRepositoryImpl;
//    private OrdersEntity testOrder;
//    private ProductEntity testProduct;
//    private LocalDateTime time;
//
//    @BeforeEach
//    void setup() {
//        orderItemRepositoryImpl = new OrderItemRepositoryImpl(orderItemJpaRepository);
//
//        UserEntity testUser = UserEntity.builder()
//            .uuid("test-uuid1")
//            .nickname("test1")
//            .role(Role.USER)
//            .build();
//
//        UserEntity testSeller = UserEntity.builder()
//            .uuid("test-uuid2")
//            .nickname("test2")
//            .role(Role.SELLER)
//            .build();
//
//        testOrder = OrdersEntity.builder()
//            .userEntity(testUser)
//            .totalPrice(BigDecimal.valueOf(5000))
//            .orderNumber("ORD-999")
//            .status(OrderStatus.PENDING)
//            .build();
//
//        testProduct = ProductEntity.builder()
//            .name("Test Product")
//            .sellerEntity(testSeller)
//            .price(BigDecimal.valueOf(1000))
//            .build();
//
//        time = LocalDateTime.parse("2024-08-01T00:00:00");
//
//        em.persist(testUser);
//        em.persist(testOrder);
//        em.persist(testProduct);
//        em.flush();
//    }
//
//    @Test
//    void testFindAllByOrdersId() {
//        // given
//        OrderItem orderItem1 = OrderItem.builder()
//            .orders(testOrder.toModel())
//            .product(testProduct.toModel())
//            .price(BigDecimal.valueOf(1000))
//            .quantity(2)
//            .createdAt(time)
//            .deleteAt(null)
//            .build();
//
//        OrderItem orderItem2 = OrderItem.builder()
//            .orders(testOrder.toModel())
//            .product(testProduct.toModel())
//            .price(BigDecimal.valueOf(2000))
//            .quantity(3)
//            .createdAt(time)
//            .deleteAt(null)
//            .build();
//
//        orderItemRepositoryImpl.saveAll(List.of(orderItem1, orderItem2));
//
//        // when
//        List<OrderItem> orderItems = orderItemRepositoryImpl.findAllByOrdersId(testOrder.getId());
//
//        // then
//        assertThat(orderItems).hasSize(2);
//        assertThat(orderItems.get(0).getOrders().getId()).isEqualTo(testOrder.getId());
//        assertThat(orderItems.get(0).getProduct().getId()).isEqualTo(testProduct.getId());
//        assertThat(orderItems.get(0).getPrice()).isEqualTo(BigDecimal.valueOf(1000));
//        assertThat(orderItems.get(0).getQuantity()).isEqualTo(2);
//        assertThat(orderItems.get(0).getCreatedAt()).isEqualTo(time);
//        assertThat(orderItems.get(0).getDeleteAt()).isNull();
//
//    }
//
//    @Test
//    void testSaveAll() {
//        // given
//        OrderItem orderItem = OrderItem.builder()
//            .orders(testOrder.toModel())
//            .product(testProduct.toModel())
//            .price(BigDecimal.valueOf(1000))
//            .quantity(2)
//            .createdAt(time)
//            .deleteAt(null)
//            .build(
//            );
//
//        // when
//        List<OrderItem> savedOrderItems = orderItemRepositoryImpl.saveAll(List.of(orderItem));
//
//        // then
//        assertThat(orderItem.getId()).isEqualTo(savedOrderItems.get(0).getId());
//        assertThat(orderItem.getQuantity()).isEqualTo(2);
//        List<OrderItem> foundItems = orderItemRepositoryImpl.findAllByOrdersId(testOrder.getId());
//        assertThat(foundItems).isNotEmpty();
//    }
//}
