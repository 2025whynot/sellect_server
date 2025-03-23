package com.sellect.server.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.jwt.JwtFilter;
import com.sellect.server.common.resolver.AuthenticationResolver;
import com.sellect.server.config.JsonConfig;
import com.sellect.server.config.SecurityConfig;
import com.sellect.server.order.application.OrderService;
import com.sellect.server.order.controller.response.OrderDetailGetResponse;
import com.sellect.server.order.controller.response.OrderGetResponse;
import com.sellect.server.order.controller.response.OrderItemGetResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = OrderController.class)
@Import(JsonConfig.class)
@ImportAutoConfiguration(
    exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        SecurityConfig.class,
        SecurityFilterChain.class
    })
class OrderControllerTest {

    @MockBean
    JwtFilter jwtFilter;
    @MockBean
    SecurityFilterChain securityFilterChain;
    @MockBean
    AuthenticationResolver authenticationResolver;
    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws ServletException, IOException {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
            any(FilterChain.class));
    }

//    @Nested
//    @DisplayName("OrderController API 테스트")
//    class OrderTests {
//
//        @Test
//        @WithMockUser(username = "testUser", roles = {"USER"})
//        @DisplayName("결제 준비 요청")
//        void testReadyPayment() throws Exception {
//            // Given
//            Long orderId = 100L;
//            Long couponId = 50L;
//            String redirectUrl = "https://kakao.pay/payment/redirect";
//
//            when(orderService.payOrder(any(User.class), anyLong(), anyLong()))
//                .thenReturn(redirectUrl);
//
//            // When & Then
//            mockMvc.perform(
//                    MockMvcRequestBuilders.post("/api/v1/order/payment/{orderId}/ready", orderId)
//                        .param("coupon_id", String.valueOf(couponId))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.is_success").value(true))
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.result").value(redirectUrl));
//        }

//        @Test
//        @WithMockUser(username = "testUser", roles = {"USER"})
//        @DisplayName("주문 생성")
//        void testRegisterPendingOrder() throws Exception {
//            // Given
//            OrderItemAddRequest itemRequest = new OrderItemAddRequest(1L, "1000",
//                5);
//            List<OrderItemAddRequest> orderItems = List.of(
//                itemRequest);
//
//            OrderAddRequest request = new OrderAddRequest("5000", orderItems);
//
//            PendingOrderRegisterResponse response = new PendingOrderRegisterResponse(100L);
//            when(orderService.registerPendingOrder(any(User.class), any(OrderAddRequest.class)))
//                .thenReturn(response);
//
//            // When & Then
//            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/order/pending")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(new ObjectMapper().writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.is_success").value(true))
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.result.order_id").value(100));
//        }

        @Test
        @WithMockUser(username = "testUser", roles = {"USER"})
        @DisplayName("주문 페이지 조회 (결제 전)")
        void testReadPending() throws Exception {
            // Given
            Long orderId = 1L;
            OrderItemGetResponse itemResponse = new OrderItemGetResponse(
                1L,                  // productId
                "TestBrand",         // brandName
                "TestProduct",       // productName
                new BigDecimal("1500"), // productPrice
                2,                   // quantity
                "http://example.com/image.jpg" // imageUrl
            );
            List<OrderItemGetResponse> responseList = List.of(itemResponse);

            when(orderService.readPending(any(User.class), any(Long.class)))
                .thenReturn(responseList);

            // When & Then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/pending", orderId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result[0].product_id").value(1))
                .andExpect(jsonPath("$.result[0].brand_name").value("TestBrand"))
                .andExpect(jsonPath("$.result[0].product_name").value("TestProduct"))
                .andExpect(jsonPath("$.result[0].product_price").value(1500))
                .andExpect(jsonPath("$.result[0].quantity").value(2))
                .andExpect(
                    jsonPath("$.result[0].image_url").value("http://example.com/image.jpg"));
        }

        @Test
        @WithMockUser(username = "testUser", roles = {"USER"})
        @DisplayName("주문 내역 확인")
        void testGetOrdersByUser() throws Exception {
            // Given
            OrderItemGetResponse orderItem = new OrderItemGetResponse(1L, "테스트 브랜드", "테스트 상품",
                new BigDecimal("2000.00"), 2, "https://example.com/image.jpg");
            OrderGetResponse orderResponse = new OrderGetResponse(100L, List.of(orderItem),
                LocalDateTime.now());
            when(orderService.getOrdersByUser(any(User.class))).thenReturn(List.of(orderResponse));

            // When & Then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result[0].order_id").value(100))
                .andExpect(jsonPath("$.result[0].order_items[0].product_id").value(1))
                .andExpect(jsonPath("$.result[0].order_items[0].brand_name").value("테스트 브랜드"))
                .andExpect(jsonPath("$.result[0].order_items[0].product_name").value("테스트 상품"))
                .andExpect(jsonPath("$.result[0].order_items[0].product_price").value(2000.00))
                .andExpect(jsonPath("$.result[0].order_items[0].quantity").value(2))
                .andExpect(jsonPath("$.result[0].order_items[0].image_url").value(
                    "https://example.com/image.jpg"))
                .andExpect(jsonPath("$.result[0].update_at").exists());
        }

        @Test
        @WithMockUser(username = "testUser", roles = {"USER"})
        @DisplayName("주문 상세 조회")
        void testGetOrderDetail() throws Exception {
            // Given
            OrderDetailGetResponse orderDetail = new OrderDetailGetResponse("ORD12345",
                new BigDecimal("500.00"), new BigDecimal("2500.00"), List.of(),
                LocalDateTime.now());
            when(orderService.getOrderDetail(anyLong())).thenReturn(orderDetail);

            // When & Then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/100")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.order_number").value("ORD12345"))
                .andExpect(jsonPath("$.result.discount_cost").value(500.00))
                .andExpect(jsonPath("$.result.total_price").value(2500.00))
                .andExpect(jsonPath("$.result.order_items").isEmpty())
                .andExpect(jsonPath("$.result.update_at").exists());
        }
    }

