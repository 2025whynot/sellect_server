package com.sellect.server.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sellect.server.auth.domain.User;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.FakeBrandRepository;
import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.FakeCategoryRepository;
import com.sellect.server.product.controller.request.ProductModifyRequest;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductModifyResponse;
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
    private final FakeBrandRepository brandRepository = new FakeBrandRepository();
    private final FakeCategoryRepository categoryRepository = new FakeCategoryRepository();
    private final ProductService sut = new ProductService(productRepository, brandRepository, categoryRepository);

    @BeforeEach
    void setUp() {
        productRepository.clear();
        brandRepository.clear();
        categoryRepository.clear();
    }

    @Nested
    @DisplayName("상품 등록 테스트")
    class RegisterMultiple {
        User seller = User.builder()
            .id(1L)
            .build();


        @Test
        @DisplayName("상품 등록 성공")
        void test1000() {
            // Given
            Category savedCategory = categoryRepository.save(Category.builder()
                .id(10L)
                .build());

            Brand savedBrand = brandRepository.save(Brand.builder()
                .id(1L)
                .build());

            List<ProductRegisterRequest> requests = List.of(
                new ProductRegisterRequest(savedCategory.getId(), savedBrand.getId(), "10000", "상품A", 10),
                new ProductRegisterRequest(savedCategory.getId(), savedBrand.getId(), "20000", "상품B", 20)
            );

            // When
            ProductRegisterResponse response = sut.registerMultiple(seller, requests);

            // Then
            assertThat(response.successProducts()).hasSize(2);
            assertThat(response.failedProducts()).isEmpty();
        }

        @Test
        @DisplayName("요청 내 중복된 상품명이 존재하면 실패한다.")
        void test1() {
            // Given
            Category savedCategory = categoryRepository.save(Category.builder()
                .id(10L)
                .build());

            Brand savedBrand = brandRepository.save(Brand.builder()
                .id(1L)
                .build());


            List<ProductRegisterRequest> requests = List.of(
                new ProductRegisterRequest(savedCategory.getId(), savedBrand.getId(), "10000", "상품A", 10),
                new ProductRegisterRequest(savedCategory.getId(), savedBrand.getId(), "20000", "상품A", 20) // 중복된 상품명
            );

            // When
            ProductRegisterResponse response = sut.registerMultiple(seller, requests);

            // Then
            assertThat(response.successProducts()).hasSize(1);
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("요청 내 중복된 상품명");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리는 등록할 수 없다.")
        void test2() {
            // Given
            List<ProductRegisterRequest> requests = List.of(
                new ProductRegisterRequest(999L, 1L, "10000", "상품A", 10) // 존재하지 않는 카테고리
            );

            // When
            ProductRegisterResponse response = sut.registerMultiple(seller, requests);

            // Then
            assertThat(response.successProducts()).isEmpty();
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("존재하지 않는 카테고리");
        }

        @Test
        @DisplayName("존재하지 않는 브랜드 선택시 상품 등록할 수 없다.")
        void test3() {
            // Given
            Category savedCategory = categoryRepository.save(Category.builder()
                .id(10L)
                .build());

            List<ProductRegisterRequest> requests = List.of(
                new ProductRegisterRequest(savedCategory.getId(), 1L, "10000", "상품A", 10) // 존재하지 않는 카테고리
            );

            // When
            ProductRegisterResponse response = sut.registerMultiple(seller, requests);

            // Then
            assertThat(response.successProducts()).isEmpty();
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("존재하지 않는 브랜드");
        }

        @Test
        @DisplayName("이미 등록된 상품명이 존재하면 등록할 수 없다.")
        void test4() {
            // Given
            Category savedCategory = categoryRepository.save(Category.builder()
                .id(10L)
                .build());
            Brand savedBrand = brandRepository.save(Brand.builder()
                .id(1L)
                .build());

            productRepository.saveAll(
                List.of(
                    Product.builder()
                        .seller(seller)
                        .category(savedCategory)
                        .brand(savedBrand)
                        .price(new BigDecimal("10000"))
                        .name("상품A")
                        .stock(10)
                        .build()
                )
            );

            List<ProductRegisterRequest> requests = List.of(
                new ProductRegisterRequest(10L, 1L, "20000", "상품A", 20) // 중복된 상품명
            );

            // When
            ProductRegisterResponse response = sut.registerMultiple(seller, requests);

            // Then
            assertThat(response.successProducts()).isEmpty();
            assertThat(response.failedProducts()).hasSize(1);
            assertThat(response.failedProducts().get(0).reason()).isEqualTo("중복 상품");
        }
    }

    @Nested
    @DisplayName("상품 수정 테스트")
    class Modify {
        Category category = Category.builder()
            .id(10L)
            .build();

        Brand brand = Brand.builder()
            .id(1L)
            .build();

        User seller = User.builder()
            .id(1L)
            .build();

        @DisplayName("상품 수정 성공")
        void test1000() {
            // Given
            Long productId = 10L;

            Product existingProduct = Product.builder()
                .id(productId)
                .seller(seller)
                .category(category)
                .brand(brand)
                .price(new BigDecimal("10000"))
                .name("기존 상품")
                .stock(50)
                .build();

            productRepository.save(existingProduct);

            ProductModifyRequest request =
                ProductModifyRequest.builder()
                    .price("15000")
                    .name("수정된 상품")
                    .stock(100)
                    .build();

            // When
            ProductModifyResponse response = sut.modify(seller.getId(), productId, request);

            // Then
            assertThat(response.name()).isEqualTo("수정된 상품");
            assertThat(response.price()).isEqualByComparingTo("15000");
            assertThat(response.stock()).isEqualTo(100);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정할 경우 예외 발생")
        void test1() {
            // Given
            Long productId = 999L; // 존재하지 않는 상품

            ProductModifyRequest request = new ProductModifyRequest(
                "15000",
                "수정된 상품",
                100
            );

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.modify(seller.getId(), productId, request));

            assertThat(exception.getMessage()).isEqualTo("상품이 존제하지 않습니다.");
        }

        @Test
        @DisplayName("본인의 상품이 아닌 경우 수정 불가")
        void test2() {
            // Given
            Long anotherSellerId = 2L;
            User anotherSeller = User.builder().id(anotherSellerId)
                .build();


            Long productId = 10L;

            Product existingProduct = Product.builder()
                .id(productId)
                .seller(anotherSeller) // 다른 판매자의 상품
                .category(category)
                .brand(brand)
                .price(new BigDecimal("10000"))
                .name("기존 상품")
                .stock(50)
                .build();

            productRepository.save(existingProduct);

            ProductModifyRequest request = new ProductModifyRequest(
                "15000",
                "수정된 상품",
                100
            );

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.modify(seller.getId(), productId, request));

            assertThat(exception.getMessage()).isEqualTo("상품을 수정할 권한이 없습니다.");
        }

        @Test
        @DisplayName("수정할 값이 없는 경우 기존 값 유지")
        void test3() {
            // Given
            Long productId = 10L;

            Product existingProduct = Product.builder()
                .id(productId)
                .seller(seller)
                .category(category)
                .brand(brand)
                .price(new BigDecimal("10000"))
                .name("기존 상품")
                .stock(50)
                .build();

            productRepository.save(existingProduct);

            ProductModifyRequest request = new ProductModifyRequest(
                null, // 가격 변경 없음
                null, // 이름 변경 없음
                null  // 재고 변경 없음
            );

            // When
            ProductModifyResponse response = sut.modify(seller.getId(), productId, request);

            // Then
            assertThat(response.name()).isEqualTo("기존 상품");
            assertThat(response.price()).isEqualByComparingTo("10000");
            assertThat(response.stock()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("상품 삭제 테스트")
    class Remove {

        Category category = Category.builder()
            .id(10L)
            .build();

        Brand brand = Brand.builder()
            .id(1L)
            .build();

        User seller = User.builder()
            .id(1L)
            .build();

        @Test
        @DisplayName("상품 삭제 성공")
        void test1000() {
            // Given
            Long sellerId = 1L;
            Long productId = 10L;

            Product existingProduct = Product.builder()
                .id(productId)
                .seller(seller)
                .category(category)
                .brand(brand)
                .price(new BigDecimal("10000"))
                .name("기존 상품")
                .stock(50)
                .build();

            productRepository.save(existingProduct);

            // When
            sut.remove(seller.getId(), productId);

            // Then
            assertThat(productRepository.findById(productId)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제 시 예외 발생")
        void test1() {
            // Given
            Long sellerId = 1L;
            Long productId = 999L; // 존재하지 않는 상품

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.remove(sellerId, productId));

            assertThat(exception.getMessage()).isEqualTo("상품이 존제하지 않습니다.");
        }

        @Test
        @DisplayName("본인의 상품이 아닌 경우 삭제 불가")
        void test2() {
            // Given
            Long anotherSellerId = 2L;
            User anotherSeller = User.builder()
                .id(anotherSellerId)
                .build();

            Long productId = 10L;

            Product existingProduct = Product.builder()
                .id(productId)
                .seller(anotherSeller) // 다른 판매자의 상품
                .category(category)
                .brand(brand)
                .price(new BigDecimal("10000"))
                .name("기존 상품")
                .stock(50)
                .build();

            productRepository.save(existingProduct);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.remove(seller.getId(), productId));

            assertThat(exception.getMessage()).isEqualTo("상품을 수정할 권한이 없습니다.");
        }
    }
}
