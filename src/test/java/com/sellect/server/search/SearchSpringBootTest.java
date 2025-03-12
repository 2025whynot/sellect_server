 //package com.sellect.server.search;
//
//import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doAnswer;
//
//import com.sellect.server.common.infrastructure.jwt.JwtFilter;
//import com.sellect.server.common.response.ApiResponse;
//import com.sellect.server.common.response.PagedResponse;
//import com.sellect.server.config.BlazePersistenceConfig;
//import com.sellect.server.config.BlazeQueryDslConfig;
//import com.sellect.server.config.SecurityConfig;
//import com.sellect.server.config.TestRestTemplateConfig;
//import com.sellect.server.product.application.S3StorageClient;
//import com.sellect.server.search.controller.response.SearchResponse;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import java.io.IOException;
//import java.math.BigDecimal;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
//import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.context.annotation.Import;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.test.context.support.WithAnonymousUser;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.test.context.TestPropertySource;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Import({
//    TestRestTemplateConfig.class,
//    BlazePersistenceConfig.class,
//    BlazeQueryDslConfig.class})
//@ImportAutoConfiguration(
//    exclude = {
//        SecurityAutoConfiguration.class,
//        ManagementWebSecurityAutoConfiguration.class,
//        SecurityConfig.class}) // Spring Security 비활성화
//@TestPropertySource(properties = {
//    "spring.jpa.hibernate.ddl-auto=none",  // 테이블 자동 생성
//    "spring.sql.init.mode=always", // SQL 초기화 항상 수행
//    "spring.sql.init.schema-locations=classpath:schema.sql",
//    "spring.sql.init.data-locations=classpath:data.sql"
//})
//public class SearchSpringBootTest {
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
//    private TestRestTemplate restTemplate ;
//
//    @BeforeEach
//    void setup() throws ServletException, IOException {
//        baseUrl = "http://localhost:" + port;
//
//        // doFilter() 실행되도록 설정하여 FilterChain 정상 작동
//        doAnswer(invocation -> {
//            ServletRequest request = invocation.getArgument(0);
//            ServletResponse response = invocation.getArgument(1);
//            FilterChain chain = invocation.getArgument(2);
//            chain.doFilter(request, response);
//            return null;
//        }).when(jwtFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
//            any(FilterChain.class));
//    }
//
//    @Test
//    @DisplayName("상품 검색 API - 회원 요청 (200 OK)")
//    void testSearchProductWithTestRestTemplate() {
//        // Given: 상품 조회 파라미터 설정
//        String keyword = "Product1";
//        int page = 0;
//        int size = 20;
//
//        // 상품 조회 API URL
//        String url = baseUrl + "/api/v1/search/products?keyword=" + keyword +
//            "&page=" + page + "&size=" + size + "&sortType=LATEST";
//
//        // When: 실제 요청을 보내고 응답을 받음
//        ResponseEntity<ApiResponse<PagedResponse<SearchResponse>>> response = restTemplate.exchange(
//            url,
//            org.springframework.http.HttpMethod.GET,
//            null,
//            new ParameterizedTypeReference<ApiResponse<PagedResponse<SearchResponse>>>() {} // 명시적 타입 지정
//        );
//
//        //  API 응답 직접 출력 (디버깅)
//        System.out.println("Response Status: " + response.getStatusCode());
//        System.out.println("Response Body: " + response.getBody());
//
//        // API 응답이 NULL이면 테스트 실패
//        assertNotNull(response.getBody(), "API 응답이 null입니다. API가 정상적으로 동작하는지 확인하세요.");
//
//        //  응답 상태 코드 검증
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "API 응답 코드가 200이 아닙니다.");
//        assertEquals(true, response.getBody().isSuccess(), "API 응답 isSuccess 값이 null 또는 false 입니다.");
//        assertEquals(200, response.getBody().status(), "API 응답 status 값이 200이 아닙니다.");
//
//        // 응답 내용 검증
//        PagedResponse<SearchResponse> pagedResponse = response.getBody().result();
//        assertNotNull(pagedResponse, "PagedResponse가 null입니다.");
//        assertFalse(pagedResponse.content().isEmpty(), "조회된 상품이 없습니다.");
//
//        SearchResponse product = pagedResponse.content().get(0);
//        assertEquals("Brand1", product.brandName(), "브랜드명이 다릅니다.");
//        assertEquals("1", product.productId(), "상품 ID가 다릅니다.");
//        assertEquals("http://example.com/image.jpg", product.imageUrl(), "이미지 URL이 다릅니다.");
//        assertEquals("Product1", product.name(), "상품명이 다릅니다.");
//        assertEquals(new BigDecimal("55000.00"), product.price(), "가격이 다릅니다.");
//
//        // 페이징 정보 검증
//        assertEquals(0, pagedResponse.pageNumber());
//        assertEquals(20, pagedResponse.pageSize());
//        assertEquals(1, pagedResponse.totalElements());
//        assertEquals(1, pagedResponse.totalPages());
//        assertTrue(pagedResponse.isLast());
//    }
//
//    @Test
//    @WithAnonymousUser
//    @DisplayName("상품 검색 API - 비회원 요청 (200 OK)")
//    void testSearchProductWithTestRestTemplateAnonymous() {
//        String keyword = "Product1";
//        int page = 0;
//        int size = 20;
//
//        String url = baseUrl + "/api/v1/search/products?keyword=" + keyword +
//            "&page=" + page + "&size=" + size + "&sortType=LATEST";
//
//        ResponseEntity<ApiResponse<PagedResponse<SearchResponse>>> response = restTemplate.exchange(
//            url,
//            org.springframework.http.HttpMethod.GET,
//            null,
//            new ParameterizedTypeReference<>() {}
//        );
//
//        // API 응답 확인
//        System.out.println("Response: " + response.getBody());
//
//        // 응답 상태 코드 검증
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(true, response.getBody().isSuccess(), "API 응답 isSuccess 값이 null 또는 false 입니다.");
//        assertEquals(200, response.getBody().status());
//
//        PagedResponse<SearchResponse> pagedResponse = response.getBody().result();
//        assertEquals(1, pagedResponse.content().size(), "조회된 상품 개수가 다릅니다.");
//
//        SearchResponse product = pagedResponse.content().get(0);
//        assertEquals("Brand1", product.brandName());
//        assertEquals("1", product.productId());
//        assertEquals("http://example.com/image.jpg", product.imageUrl());
//        assertEquals("Product1", product.name());
//        assertEquals(new BigDecimal("55000.00"), product.price());
//
//        assertEquals(0, pagedResponse.pageNumber());
//        assertEquals(20, pagedResponse.pageSize());
//        assertEquals(1, pagedResponse.totalElements());
//        assertEquals(1, pagedResponse.totalPages());
//        assertTrue(pagedResponse.isLast());
//    }
//}
