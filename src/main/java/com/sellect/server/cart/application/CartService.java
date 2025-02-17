package com.sellect.server.cart.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.cart.controller.request.CartItemAddRequest;
import com.sellect.server.cart.controller.request.CartItemQuantityChangeRequest;
import com.sellect.server.cart.domain.CartItem;
import com.sellect.server.cart.repository.CartItemRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;


    @Transactional
    public void addCartItem(User user, CartItemAddRequest request) {

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));
        CartItem cartItem = CartItem.register(user, product, request.quantity());
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void changeCartItemQuantity(Long userId, CartItemQuantityChangeRequest request) {

        CartItem cartItem = cartItemRepository.findById(request.cartItemId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "cart item"));
        if (!Objects.equals(cartItem.getUser().getId(), userId)) {
            throw new CommonException(BError.FAIL_FOR_REASON,
                "change cart item qunatity",
                "user doesn't have permission to change cart item");
        }

        cartItemRepository.save(cartItem.changeQuantity(request.quantity()));
    }

    @Transactional(readOnly = true)
    public List<CartItem> retrieveCartItems(Long userId) {
        return cartItemRepository.findAllByUserId(userId);
    }

    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {

        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "cart item"));
        if (!Objects.equals(cartItem.getUser().getId(), userId)) {
            throw new CommonException(BError.FAIL_FOR_REASON,
                "delete cart item",
                "user doesn't have permission to delete cart item");
        }
        cartItemRepository.save(cartItem.remove());
    }

}
