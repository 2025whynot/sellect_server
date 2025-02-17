package com.sellect.server.cart.repository;

import com.sellect.server.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemRepository {

    private final CartItemJpaRepository cartItemJpaRepository;

    @Override
    public void save(CartItem cartItem) {
        cartItemJpaRepository.save(CartItemEntity.from(cartItem));
    }

    @Override
    public Optional<CartItem> findById(Long cartItemId) {
        return cartItemJpaRepository.findById(cartItemId)
            .map(CartItemEntity::toModel);
    }

    @Override
    public List<CartItem> findAllByUserId(Long userId) {
        return cartItemJpaRepository.findByUserId(userId).stream()
            .map(CartItemEntity::toModel)
            .toList();
    }
}
