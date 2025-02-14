package com.sellect.server.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.FakeUserRepository;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.FakeBrandRepository;
import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.FakeCategoryRepository;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.FakeProductRepository;
import com.sellect.server.review.controller.request.ReviewRegisterRequest;
import com.sellect.server.review.domain.Review;
import com.sellect.server.review.repository.FakeReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {

    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final FakeCategoryRepository categoryRepository = new FakeCategoryRepository();
    private final FakeBrandRepository brandRepository = new FakeBrandRepository();
    private final FakeReviewRepository reviewRepository = new FakeReviewRepository();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final ReviewService sut = new ReviewService(reviewRepository, productRepository);

    @BeforeEach
    void setUp() {
        reviewRepository.clear();
        productRepository.clear();
    }

    @Nested
    @DisplayName("리뷰 등록 테스트")
    class Register {

        @Test
        @DisplayName("리뷰 등록 성공")
        void test1000() {
            // Given
            User savedUser = userRepository.save(User.builder().id(1L).build());

            User savedSeller = userRepository.save(
                User.builder()
                    .id(2L)
                    .build());

            Category savedCategory = categoryRepository.save(Category
                .builder()
                .id(1L)
                .build());

            Brand savedBrand = brandRepository.save(Brand
                .builder()
                .id(1L)
                .build());

            Product savedProduct = productRepository.save(Product.builder()
                .id(10L)
                .seller(savedSeller)
                .category(savedCategory)
                .brand(savedBrand)
                .build());

            ReviewRegisterRequest request = new ReviewRegisterRequest(savedProduct.getId(), 5,
                "좋은 상품입니다.");

            // When
            Review review = sut.register(savedUser, request);

            // Then
            assertThat(review.getUser().getId()).isEqualTo(savedUser.getId());
            assertThat(review.getProduct().getId()).isEqualTo(savedProduct.getId());
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getDescription()).isEqualTo("좋은 상품입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 상품에 대한 리뷰 등록 실패")
        void test1() {
            // Given
            User user = User.builder().id(1L).build();
            System.out.println("user = " + user);
            User savedUser = userRepository.save(user);
            System.out.println("savedUser = " + savedUser);
            ReviewRegisterRequest request = new ReviewRegisterRequest(999L, 5, "존재하지 않는 상품 리뷰");

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.register(user, request));
            assertThat(exception.getMessage()).isEqualTo("존재하지 않는 상품입니다.");
        }
    }
}