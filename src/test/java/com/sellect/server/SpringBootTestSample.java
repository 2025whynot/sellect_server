package com.sellect.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.sellect.server.common.infrastructure.jwt.JwtFilter;
import com.sellect.server.config.SecurityConfig;
import com.sellect.server.config.TestRestTemplateConfig;
import com.sellect.server.product.application.S3StorageClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;

//@EnableJpaAuditing // Entity @LastModifiedDate 같은 JPA Auditing 기능 활성화  -- 이미 main 코드에 존재
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestRestTemplateConfig.class}) // 필수!! 매우 중요!
@ImportAutoConfiguration(
    exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        SecurityConfig.class}) // Spring Security 비활성화
class SpringBootTestSample {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @MockBean
    JwtFilter jwtFilter;

    @MockBean
    S3StorageClient s3StorageClient;

    @MockBean
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private TestRestTemplate restTemplate ;

    @BeforeEach
    void setup() throws ServletException, IOException {
        baseUrl = "http://localhost:" + port;

        // 🔹 doFilter() 실행되도록 설정하여 FilterChain 정상 작동
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
            any(FilterChain.class));
    }

//    @Test
//    void contextLoads() {
//    }

}
