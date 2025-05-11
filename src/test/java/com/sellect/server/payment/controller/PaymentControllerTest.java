package com.sellect.server.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.infrastructure.jwt.JwtFilter;
import com.sellect.server.common.resolver.AuthenticationResolver;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.config.JsonConfig;
import com.sellect.server.config.SecurityConfig;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import jakarta.servlet.FilterChain;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class)
@Import(JsonConfig.class) // JSON 직렬화 설정 적용
@ImportAutoConfiguration(
    exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        SecurityConfig.class,
        SecurityFilterChain.class

    }) // Spring Security 비활성화
class PaymentControllerTest {

    @MockBean
    JwtFilter jwtFilter;

    @MockBean
    SecurityFilterChain securityFilterChain;

    @MockBean
    AuthenticationResolver authenticationResolver;
    @MockBean
    org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() throws Exception {
        mockUser = User.builder().id(1L).nickname("testUser").build(); // User 필드에 따라 조정
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        doNothing().when(jwtFilter).doFilter(any(), any(), any()); // 필터 통과
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class).doFilter(
                invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }


    @Nested
    @DisplayName("getPaymentHistory()")
    class PaymentHistoryController{
        @Test
        @DisplayName("[성공] 결제 내역 정보 가져오기")
        void willSuccess() throws Exception {
            // given
            // Pageable 설정 (기본값: page=0, size=5, sort=createdAt,desc)

            // PaymentHistoryResponse 샘플 데이터
            PaymentHistoryResponse history1 = new PaymentHistoryResponse(1L, "1000", "3", "test_pid1", "APPROVE", "2025-02-25");
            PaymentHistoryResponse history2 = new PaymentHistoryResponse(2L, "2000", "5", "test_pid2", "APPROVE", "2025-03-01");
            List<PaymentHistoryResponse> paymentHistory = List.of(history1, history2);

            // PaymentService가 paymentHistory를 반환하도록 설정
            when(paymentService.getPaymentHistory(any(User.class), any(Pageable.class))).thenReturn(paymentHistory);

            // ApiResponse 생성
            ApiResponse<List<PaymentHistoryResponse>> apiResponse = ApiResponse.ok(paymentHistory);
            String expectedJson = objectMapper.writeValueAsString(apiResponse);

            // when & then
            mockMvc.perform(get("/api/v1/payment/history")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
        }
    }
}
