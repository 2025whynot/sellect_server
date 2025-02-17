package com.sellect.server.cart.repository;

import com.sellect.server.cart.domain.CartItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeCartItemRepository implements CartItemRepository {
    private final Map<Long, CartItem> data = new HashMap<>();

    @Override
    public void save(CartItem cartItem) {
        data.put(cartItem.getId(), cartItem);
    }

    @Override
    public Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<CartItem> findAllByUserId(Long userId) {
        List<CartItem> result = new ArrayList<>();
        for (CartItem item : data.values()) {
            if (item.getUser().getId().equals(userId)) {
                result.add(item);
            }
        }
        return result;
    }
}
