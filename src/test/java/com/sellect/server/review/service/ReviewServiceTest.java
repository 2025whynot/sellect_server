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
import com.sellect.server.review.controller.request.ReviewModifyRequest;
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

    @Nested
    @DisplayName("리뷰 수정 테스트")
    class Modify {

        @Test
        @DisplayName("리뷰 수정 성공")
        void test1000() {
            // Given
            User user = userRepository.save(
                User.builder().id(1L).uuid("user-uuid").nickname("test-user").build());
            Product product = productRepository.save(Product.builder().id(10L).build());

            Review review = reviewRepository.save(Review.register(user, product, 4, "괜찮아요."));
            ReviewModifyRequest request = new ReviewModifyRequest(review.getId(), 5F, "정말 좋아요!");

            // When
            Review modifiedReview = sut.modify(user, review.getId(), request);

            // Then
            assertThat(modifiedReview.getRating()).isEqualTo(5);
            assertThat(modifiedReview.getDescription()).isEqualTo("정말 좋아요!");
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 수정 시 예외 발생")
        void test1() {
            // Given
            User user = userRepository.save(
                User.builder().id(1L).uuid("user-uuid").nickname("test-user").build());
            ReviewModifyRequest request = new ReviewModifyRequest(999L, 5F, "좋아요!");

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.modify(user, 999L, request));
            assertThat(exception.getMessage()).isEqualTo("존재하지 않는 리뷰입니다.");
        }

        @Test
        @DisplayName("본인이 작성하지 않은 리뷰 수정 시 예외 발생")
        void test2() {
            // Given
            User owner = userRepository.save(
                User.builder().id(1L).uuid("owner-uuid").nickname("owner-user").build());
            User anotherUser = userRepository.save(
                User.builder().id(2L).uuid("other-uuid").nickname("other-user").build());

            Product product = productRepository.save(Product.builder().id(10L).build());
            Review review = reviewRepository.save(Review.register(owner, product, 4, "괜찮아요."));

            ReviewModifyRequest request = new ReviewModifyRequest(review.getId(), 5F, "정말 좋아요!");

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sut.modify(anotherUser, review.getId(), request));
            assertThat(exception.getMessage()).isEqualTo("리뷰 수정 권한이 없습니다.");
        }

        @Test
        @DisplayName("수정할 값이 없을 경우 기존 값 유지")
        void test3() {
            // Given
            User user = userRepository.save(
                User.builder().id(1L).uuid("user-uuid").nickname("test-user").build());
            Product product = productRepository.save(Product.builder().id(10L).build());

            Review review = reviewRepository.save(Review.register(user, product, 4, "괜찮아요."));
            ReviewModifyRequest request = new ReviewModifyRequest(review.getId(), null,
                null); // 값 없음

            // When
            Review modifiedReview = sut.modify(user, review.getId(), request);

            // Then
            assertThat(modifiedReview.getRating()).isEqualTo(4); // 기존 값 유지
            assertThat(modifiedReview.getDescription()).isEqualTo("괜찮아요."); // 기존 값 유지
        }
    }

        @Nested
    @DisplayName("리뷰 삭제 테스트")
    class Remove {

        @Test
        @DisplayName("리뷰 삭제 성공")
        void test1000() {
            // Given
            User user = User.builder().id(1L).uuid("test-uuid").nickname("test-user").build();
            User savedUser = userRepository.save(user);

            Product product = productRepository.save(Product.builder().id(10L).build());

            Review review = reviewRepository.save(Review.register(savedUser, product, 5, "좋은 상품입니다."));

            // When
            sut.remove(savedUser, review.getId());

            // Then
            assertThat(reviewRepository.findById(review.getId())).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 삭제 시 예외 발생")
        void test1() {
            // Given
            User user = User.builder().id(1L).uuid("test-uuid").nickname("test-user").build();
            User savedUser = userRepository.save(user);

            Long nonExistingReviewId = 999L; // 존재하지 않는 리뷰 ID

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> sut.remove(savedUser, nonExistingReviewId));
            assertThat(exception.getMessage()).isEqualTo("존재하지 않는 리뷰입니다.");
        }

        @Test
        @DisplayName("본인의 리뷰가 아닌 경우 삭제 불가")
        void test2() {
            // Given
            User owner = User.builder().id(1L).uuid("owner-uuid").nickname("owner-user").build();
            User savedOwner = userRepository.save(owner);

            User anotherUser = User.builder().id(2L).uuid("other-uuid").nickname("other-user").build();
            User savedAnotherUser = userRepository.save(anotherUser);

            Product product = productRepository.save(Product.builder().id(10L).build());

            Review review = reviewRepository.save(Review.register(savedOwner, product, 5, "좋은 상품입니다."));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> sut.remove(savedAnotherUser, review.getId()));
            assertThat(exception.getMessage()).isEqualTo("리뷰 삭제 권한이 없습니다.");
        }
    }
}