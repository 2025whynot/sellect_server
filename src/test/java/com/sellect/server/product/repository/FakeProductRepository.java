package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductSearchCondition;
import com.sellect.server.product.domain.ProductSortType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FakeProductRepository implements ProductRepository {

    private final List<Product> data = new ArrayList<>();

    // todo : 상품 조회 테스트 작성 시 구현
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    @Override
    public List<Product> search(ProductSearchCondition condition, int page, int size,
        ProductSortType sortType) {
        return null;
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        data.addAll(products);
        return new ArrayList<>(data);
    }

    @Override
    public boolean isDuplicateProduct(Long sellerId, String name) {
        return data.stream()
            .anyMatch(product -> product.getSeller().getId().equals(sellerId) &&
                product.getName().equals(name) && product.getDeleteAt() == null);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return data.stream()
            .filter(product -> product.getId().equals(productId))
            .filter(product -> product.getDeleteAt() == null)
            .findFirst();
    }

    @Override
    public Product save(Product product) {
        findById(product.getId()).ifPresentOrElse(
            existingProduct -> {
                // 기존 데이터 업데이트 (삭제 후 재등록)
                data.remove(existingProduct);
                data.add(product);
            },
            () -> data.add(product) // 새로운 데이터 추가
        );

        return product;
    }

    @Override
    public Page<Product> findContainingName(String keyword, Pageable pageable) {
        List<Product> findProducts = data.stream()
            .filter(product -> product.getName().contains(keyword))
            .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        List<Product> findProducts = data.stream()
            .filter(product -> product.getCategory().getId().equals(categoryId))
            .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
        List<Product> findProducts = data.stream()
            .filter(product -> product.getBrand().getId().equals(brandId))
            .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findByIdIn(List<Long> ids, Pageable pageable) {
        List<Product> findProducts = data.stream()
            .filter(product -> ids.contains(product.getId()))
            .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    public List<Product> findAll() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
    }

}