package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FakeProductImageRepository implements ProductImageRepository {

    private final Map<Long, Map<String, ProductImage>> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ProductImage productImage, Product product) {
        storage.computeIfAbsent(product.getId(), k -> new HashMap<>())
            .put(productImage.getUuid(), productImage);
    }

    @Override
    public Optional<ProductImage> findByProductIdAndUuid(Long productId, String uuid) {
        Optional<ProductImage> productImage = Optional.ofNullable(
            storage.getOrDefault(productId, Collections.emptyMap()).get(uuid));
        return productImage.isEmpty() ? Optional.empty()
            : productImage.get().getDeleteAt() == null ? productImage : Optional.empty();
    }

    @Override
    public List<ProductImage> findByProductId(Long productId) {
        return new ArrayList<>(storage.getOrDefault(productId, Collections.emptyMap()).values());
    }
}
