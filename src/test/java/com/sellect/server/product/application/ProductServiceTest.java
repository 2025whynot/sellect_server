package com.sellect.server.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.FakeCategoryRepository;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductRegisterResponse;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.FakeProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final FakeCategoryRepository categoryRepository = new FakeCategoryRepository();
    private final ProductService productService = new ProductService(productRepository,
            categoryRepository);

    @BeforeEach
    void setUp() {
        productRepository.clear();
        categoryRepository.clear();
    }

    @Nested
    @DisplayName("상품 등록 테스트")
    class RegisterMultiple {

        @Test
        @DisplayName("상품 등록 성공")
        void test1000() {
            // Given
            Long sellerId = 1L;
            categoryRepository.save(Category.builder()
                    .id(10L)
                    .build());

            List<ProductRegisterRequest> requests = List.of(
                    new ProductRegisterRequest(10L, 1L, "10000", "상품A", 10),
                    new ProductRegisterRequest(10L, 2L, "20000", "상품B", 20)
            );

            // When
            ProductRegisterResponse response = productService.registerMultiple(sellerId, requests);

            // Then
            assertThat(response.successProducts()).hasSize(2);
            assertThat(response.failedProducts()).isEmpty();
        }

        @Test
        @DisplayName("요청 내 중복된 상품명이 존재하면 실패한다.")
        void test1() {
            // Given
            Long sellerId = 1L;
            categoryRepository.save(Category.builder()
                    .id(10L)
                    .build());

            List<ProductRegisterRequest> requests = List.of(
                    new ProductRegisterRequest(10L, 1L, "10000", "상품A", 10),
                    new ProductRegisterRequest(10L, 2L, "20000", "상품A", 20) // 중복된 상품명
            );

            // When
            ProductRegisterResponse response = productService.registerMultiple(sellerId, requests);

            // Then
            assertThat(response.successProducts()).hasSize(1);
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("요청 내 중복된 상품명");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리는 등록할 수 없다.")
        void test2() {
            // Given
            Long sellerId = 1L;

            List<ProductRegisterRequest> requests = List.of(
                    new ProductRegisterRequest(999L, 1L, "10000", "상품A", 10) // 존재하지 않는 카테고리
            );

            // When
            ProductRegisterResponse response = productService.registerMultiple(sellerId, requests);

            // Then
            assertThat(response.successProducts()).isEmpty();
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("존재하지 않는 카테고리");
        }

        @Test
        @DisplayName("이미 등록된 상품명이 존재하면 등록할 수 없다.")
        void test3() {
            // Given
            Long sellerId = 1L;
            categoryRepository.save(Category.builder()
                    .id(10L)
                    .build());

            productRepository.save(
                    Product.builder()
                            .sellerId(sellerId)
                            .categoryId(10L)
                            .brandId(1L)
                            .price(new BigDecimal("10000"))
                            .name("상품A")
                            .stock(10)
                            .build()
            );

            List<ProductRegisterRequest> requests = List.of(
                    new ProductRegisterRequest(10L, 1L, "20000", "상품A", 20) // 중복된 상품명
            );

            // When
            ProductRegisterResponse response = productService.registerMultiple(sellerId, requests);

            // Then
            assertThat(response.successProducts()).isEmpty();
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("중복 상품");
        }
    }
}