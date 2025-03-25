package com.sellect.server.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OrderItemTest {

    private Orders orders;
    private Product product;
    private BigDecimal price;
    private int quantity;

    @BeforeEach
    void setUp() {
        orders = Orders.builder()
            .id(1L)
            .user(null) // User는 OrderItem 테스트에 필요 없으므로 null로 설정
            .totalPrice(new BigDecimal("100000"))
            .orderNumber("test-order-number")
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.parse("2024-08-01T00:00:00"))
            .updatedAt(LocalDateTime.parse("2024-08-01T00:00:00"))
            .build();

        product = Product.builder()
            .id(1L)
            .name("Test Product")
            .price(new BigDecimal("50000"))
            .build();

        price = new BigDecimal("50000");
        quantity = 2;
    }

    @Test
    @DisplayName("주문 아이템 등록 성공")
    void testRegister() {
        // when
        OrderItem orderItem = OrderItem.register(orders, product, price, quantity);

        // then
        assertNotNull(orderItem);
        assertEquals(orders, orderItem.getOrders());
        assertEquals(price, orderItem.getPrice());
        assertEquals(quantity, orderItem.getQuantity());
        assertNotNull(orderItem.getCreatedAt());
        assertNull(orderItem.getDeleteAt());
    }

    @Nested
    @DisplayName("재고 차감 테스트")
    class DeductStockTest {

        @Test
        @DisplayName("재고 차감 성공")
        void testDeductStockSuccess() {
            // given
            OrderItem orderItem = OrderItem.register(orders, product, price, quantity);
            Inventory inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .stock(10)
                .build();

            // when
            Inventory updatedInventory = orderItem.deductStock(inventory);

            // then
            assertNotNull(updatedInventory);
            assertEquals(product, updatedInventory.getProduct());
            assertEquals(10 - quantity, updatedInventory.getStock()); // 10 - 2 = 8
        }

        @Test
        @DisplayName("재고 부족 시 예외 발생")
        void testDeductStockInsufficient() {
            // given
            OrderItem orderItem = OrderItem.register(orders, product, price, 5); // 수량 5
            Inventory inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .stock(3) // 재고 3
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> orderItem.deductStock(inventory));
            assertEquals("재고 부족 is not valid", exception.getMessage());
        }
    }
}