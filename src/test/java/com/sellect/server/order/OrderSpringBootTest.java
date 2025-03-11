//package com.sellect.server.order;
//
//import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doAnswer;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.sellect.server.auth.domain.User;
//import com.sellect.server.auth.repository.entity.Role;
//import com.sellect.server.auth.repository.user.UserRepository;
//import com.sellect.server.common.infrastructure.jwt.JwtFilter;
//import com.sellect.server.common.response.ApiResponse;
//import com.sellect.server.config.SecurityConfig;
//import com.sellect.server.config.TestRestTemplateConfig;
//import com.sellect.server.order.controller.response.OrderDetailGetResponse;
//import com.sellect.server.order.controller.response.OrderGetResponse;
//import com.sellect.server.order.controller.response.OrderItemGetResponse;
//import com.sellect.server.product.application.S3StorageClient;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.util.List;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
//import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.context.annotation.Import;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Import({TestRestTemplateConfig.class})
//@ImportAutoConfiguration(
//    exclude = {
//        SecurityAutoConfiguration.class,
//        ManagementWebSecurityAutoConfiguration.class,
//        SecurityConfig.class})
//@Testcontainers
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//public class OrderSpringBootTest {
//
//    @LocalServerPort
//    private int port;
//
//    private String baseUrl;
//
//    @MockBean
//    JwtFilter jwtFilter;
//
//    @MockBean
//    private SecurityFilterChain securityFilterChain;
//
//    @MockBean
//    S3StorageClient s3StorageClient;
//
//    @Autowired
//    private TestRestTemplate restTemplate;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private UserRepository userRepository;
//
//    @Container
//    private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
//        .withDatabaseName("test-db")
//        .withUsername("test")
//        .withPassword("test");
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
//        registry.add("spring.datasource.username", mysqlContainer::getUsername);
//        registry.add("spring.datasource.password", mysqlContainer::getPassword);
//        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
//        registry.add("spring.jpa.properties.hibernate.id.new_generator_mappings", () -> "false");
//    }
//
//
//    @BeforeEach
//    void setup() throws ServletException, IOException {
//        baseUrl = "http://localhost:" + port;
//
//        doAnswer(invocation -> {
//            ServletRequest request = invocation.getArgument(0);
//            ServletResponse response = invocation.getArgument(1);
//            FilterChain chain = invocation.getArgument(2);
//            chain.doFilter(request, response);
//            return null;
//        }).when(jwtFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
//            any(FilterChain.class));
//
//        User testUser = User.builder()
//            .id(1L)
//            .uuid("testUser")
//            .nickname("testUser")
//            .role(Role.USER)
//            .build();
//
//        userRepository.save(testUser);
//
//    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("결제 준비")
//    void testReadyPayment() {
//        // Given: 결제할 주문 ID 설정 (data.sql에 따라 조정 필요)
//        Long orderId = 1L; // 존재하는 주문 ID여야 함
//        Long couponId = null; // 쿠폰 사용 X
//
//        String url = baseUrl + "/api/v1/order/payment/" + orderId + "/ready";
//
//        // When: POST 요청 전송
//        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
//            url + (couponId != null ? "?coupon_id=" + couponId : ""),
//            HttpMethod.POST,
//            null,
//            new ParameterizedTypeReference<>() {
//            }
//        );
//
//        // 응답 출력 (디버깅용)
//        System.out.println("Response Status: " + response.getStatusCode());
//        System.out.println("Response Body: " + response.getBody());
//
//        // Then: 응답 검증
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "API 응답 코드가 200이 아닙니다.");
//        assertNotNull(response.getBody(), "API 응답이 null입니다.");
//
//        ApiResponse<String> body = response.getBody();
//        assertEquals(true, body.isSuccess(), "isSuccess가 true가 아닙니다.");
//        assertEquals(200, body.status(), "status가 200이 아닙니다.");
//        assertNotNull(body.result(), "결제 리디렉션 URL이 null입니다.");
//        assertFalse(body.result().isEmpty(), "리디렉션 URL이 비어 있습니다.");
//    }
//
/// /    @Test /    @WithMockUser(username = "testUser", roles = {"USER"}) /    @DisplayName("주문
/// 생성") /    void testRegisterPendingOrder() throws Exception { /        // Given: 주문 생성 요청 데이터 준비
/// /        OrderItemAddRequest itemRequest = new OrderItemAddRequest(1L, "1000", 5); /
/// List<OrderItemAddRequest> orderItems = List.of(itemRequest); /        OrderAddRequest request =
/// new OrderAddRequest("5000", orderItems); / /        String url = baseUrl +
/// "/api/v1/order/pending"; / /        // HttpHeaders 객체 생성 및 설정 /
/// org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders(); /
///    headers.setContentType(MediaType.APPLICATION_JSON); / /        // When: POST 요청 전송 /
/// HttpEntity<String> entity = new HttpEntity<>( /
/// objectMapper.writeValueAsString(request), /            headers /        ); /
/// ResponseEntity<ApiResponse<PendingOrderRegisterResponse>> response = restTemplate.exchange( /
///         url, /            HttpMethod.POST, /            entity, /            new
/// ParameterizedTypeReference<>() { /            } /        ); / /        // 응답 출력 (디버깅용) /
/// System.out.println("Response Status: " + response.getStatusCode()); /
/// System.out.println("Response Body: " + response.getBody()); / /        // Then: 응답 검증 /
/// assertEquals(HttpStatus.OK, response.getStatusCode(), "API 응답 코드가 200이 아닙니다."); /
/// assertNotNull(response.getBody(), "API 응답이 null입니다."); / /
/// ApiResponse<PendingOrderRegisterResponse> body = response.getBody(); /        assertEquals(true,
/// body.isSuccess(), "isSuccess가 true가 아닙니다."); /        assertEquals(200, body.status(), "status가
/// 200이 아닙니다."); / /        PendingOrderRegisterResponse result = body.result(); /
/// assertNotNull(result, "PendingOrderRegisterResponse가 null입니다."); /
/// assertNotNull(result.orderId(), "주문 ID가 null입니다."); /    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("주문서 페이지 조회")
//    void testReadPendingOrder() {
//        // Given: 주문 ID 설정 (data.sql에 따라 조정 필요)
//        Long orderId = 1L; // 실제 데이터베이스에 존재하는 PENDING 상태의 주문 ID
//        String url = baseUrl + "/api/v1/orders/" + orderId + "/pending";
//
//        // When: GET 요청 전송
//        ResponseEntity<ApiResponse<List<OrderItemGetResponse>>> response = restTemplate.exchange(
//            url,
//            HttpMethod.GET,
//            null,
//            new ParameterizedTypeReference<>() {
//            }
//        );
//
//        // 응답 출력 (디버깅용)
//        System.out.println("Response Status: " + response.getStatusCode());
//        System.out.println("Response Body: " + response.getBody());
//
//        // Then: 응답 검증
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "API 응답 코드가 200이 아닙니다.");
//        assertNotNull(response.getBody(), "API 응답이 null입니다.");
//
//        ApiResponse<List<OrderItemGetResponse>> body = response.getBody();
//        assertEquals(true, body.isSuccess(), "isSuccess가 true가 아닙니다.");
//        assertEquals(200, body.status(), "status가 200이 아닙니다.");
//
//        List<OrderItemGetResponse> result = body.result();
//        assertNotNull(result, "OrderItemGetResponse 리스트가 null입니다.");
//        assertFalse(result.isEmpty(), "조회된 주문 아이템이 없습니다.");
//
//        // 예시 데이터 검증 (data.sql에 따라 조정 필요)
//        OrderItemGetResponse item = result.get(0);
//        assertEquals(1L, item.productId());
//        assertEquals(new BigDecimal("1000"), item.productPrice());
//        assertEquals(5, item.quantity());
//    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("주문 목록 조회")
//    void testGetOrders() {
//        // Given: URL 설정
//        String url = baseUrl + "/api/v1/orders";
//
//        // When: GET 요청 전송
//        ResponseEntity<ApiResponse<List<OrderGetResponse>>> response = restTemplate.exchange(
//            url,
//            HttpMethod.GET,
//            null,
//            new ParameterizedTypeReference<>() {
//            }
//        );
//
//        // 응답 출력 (디버깅용)
//        System.out.println("Response Status: " + response.getStatusCode());
//        System.out.println("Response Body: " + response.getBody());
//
//        // Then: 응답 검증
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "API 응답 코드가 200이 아닙니다.");
//        assertNotNull(response.getBody(), "API 응답이 null입니다.");
//
//        ApiResponse<List<OrderGetResponse>> body = response.getBody();
//        assertEquals(true, body.isSuccess(), "isSuccess가 true가 아닙니다.");
//        assertEquals(200, body.status(), "status가 200이 아닙니다.");
//
//        List<OrderGetResponse> result = body.result();
//        assertNotNull(result, "OrderGetResponse 리스트가 null입니다.");
//        assertFalse(result.isEmpty(), "조회된 주문 내역이 없습니다.");
//    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("주문 내역 상세 조회")
//    void testGetOrderDetail() {
//        // Given: 주문 ID 설정 (data.sql에 따라 조정 필요)
//        Long orderId = 1L; // COMPLETED 상태의 주문 ID여야 함
//        String url = baseUrl + "/api/v1/orders/" + orderId;
//
//        // When: GET 요청 전송
//        ResponseEntity<ApiResponse<OrderDetailGetResponse>> response = restTemplate.exchange(
//            url,
//            HttpMethod.GET,
//            null,
//            new ParameterizedTypeReference<>() {
//            }
//        );
//
//        // 응답 출력 (디버깅용)
//        System.out.println("Response Status: " + response.getStatusCode());
//        System.out.println("Response Body: " + response.getBody());
//
//        // Then: 응답 검증
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "API 응답 코드가 200이 아닙니다.");
//        assertNotNull(response.getBody(), "API 응답이 null입니다.");
//
//        ApiResponse<OrderDetailGetResponse> body = response.getBody();
//        assertEquals(true, body.isSuccess(), "isSuccess가 true가 아닙니다.");
//        assertEquals(200, body.status(), "status가 200이 아닙니다.");
//
//        OrderDetailGetResponse result = body.result();
//        assertNotNull(result, "OrderDetailGetResponse가 null입니다.");
//        assertNotNull(result.orderNumber());
//        assertEquals(new BigDecimal("5000"), result.totalPrice()); // 데이터에 따라 조정
//        assertFalse(result.orderItems().isEmpty(), "주문 아이템이 없습니다.");
//    }
//}