package com.sellect.server.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellect.server.auth.domain.User;
import com.sellect.server.cart.controller.request.CartItemAddRequest;
import com.sellect.server.cart.controller.request.CartItemQuantityChangeRequest;
import com.sellect.server.cart.domain.CartItem;
import com.sellect.server.cart.repository.FakeCartItemRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.FakeProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    private final FakeCartItemRepository cartItemRepository = new FakeCartItemRepository();
    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final CartService sut = new CartService(cartItemRepository, productRepository);

    private final Long userId = 1L;
    private final Long productId = 1L;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
            .id(productId)
            .name("Test Product")
            .build();
        productRepository.save(product);
    }

    @Nested
    @DisplayName("카트 아이템 추가 테스트")
    class AddCartItemTests {

        @Test
        @DisplayName("카트 아이템 추가 성공")
        void addCartItem_ShouldAddCartItem() {
            // Given
            User user = User.builder().id(userId).build();
            CartItemAddRequest request = CartItemAddRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

            // When
            sut.addCartItem(user, request);

            // Then
            assertThat(cartItemRepository.findAllByUserId(userId)).hasSize(1);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외 발생")
        void addCartItem_ShouldThrowExceptionWhenProductNotFound() {
            // Given
            User user = User.builder().id(userId).build();
            CartItemAddRequest request = CartItemAddRequest.builder()
                .productId(99L)  // Non-existent product ID
                .quantity(2)
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.addCartItem(user, request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(BError.NOT_EXIST.getMessage("product"));
        }
    }

    @Nested
    @DisplayName("카트 아이템 수량 변경 테스트")
    class ChangeCartItemQuantityTests {

        @Test
        @DisplayName("카트 아이템 수량 변경 성공")
        void changeCartItemQuantity_ShouldUpdateQuantity() {
            // Given
            User user = User.builder().id(userId).build();
            CartItem cartItem = CartItem.register(user, productRepository.findById(productId).orElseThrow(), 2);
            cartItemRepository.save(cartItem);
            CartItemQuantityChangeRequest request = CartItemQuantityChangeRequest.builder()
                .cartItemId(cartItem.getId())
                .quantity(5)
                .build();

            // When
            sut.changeCartItemQuantity(userId, request);

            // Then
            assertThat(cartItemRepository.findById(cartItem.getId()).get().getQuantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("카트 아이템 조회 테스트")
    class RetrieveCartItemsTests {

        @Test
        @DisplayName("카트 아이템 조회 성공")
        void retrieveCartItems_ShouldReturnUserCartItems() {
            // Given
            User user = User.builder().id(userId).build();
            cartItemRepository.save(CartItem.register(user, productRepository.findById(productId).orElseThrow(), 2));

            // When & Then
            assertThat(sut.retrieveCartItems(userId)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("카트 아이템 삭제 테스트")
    class DeleteCartItemTests {

        @Test
        @DisplayName("카트 아이템 삭제 성공")
        void deleteCartItem_ShouldDeleteCartItem() {
            // Given
            User user = User.builder().id(userId).build();
            CartItem cartItem = CartItem.register(user, productRepository.findById(productId).orElseThrow(), 2);
            cartItemRepository.save(cartItem);

            // When
            sut.deleteCartItem(userId, cartItem.getId());

            // Then
            assertThat(cartItemRepository.findById(cartItem.getId()).get().getDeleteAt()).isNotNull();
        }
    }
}