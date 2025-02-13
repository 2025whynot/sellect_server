package com.sellect.server.product.domain;

import com.sellect.server.auth.domain.User;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.category.domain.Category;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
public class Product {

    private final Long id;
    private final User seller;
    private final Category category;
    private final Brand brand;
    private final BigDecimal price;
    private final String name;
    private final Integer stock;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static Product register(User seller, Category category, Brand brand, BigDecimal price,
        String name, Integer stock) {
        return Product.builder()
            .seller(seller)
            .category(category)
            .brand(brand)
            .price(price)
            .name(name)
            .stock(stock)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }

    // todo : 일단 KEEP
    // === 비즈니스 로직 ===

    /**
     * 상품 정보 업데이트 메서드
     */
    public Product modify(BigDecimal price, String name, Integer stock) {
        return Product.builder()
            .id(this.id)
            .seller(this.seller)
            .category(this.category)
            .brand(this.brand)
            .price(price)
            .name(name)
            .stock(stock)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now()) // 수정 시간 업데이트
            .build();
    }

    /**
     * 재고 감소 후 새로운 Product 인스턴스를 반환 (불변성 유지)
     */
    public Product updateStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalArgumentException("재고 부족");
        }
        return Product.builder()
            .id(this.id)
            .seller(this.seller)
            .category(this.category)
            .brand(this.brand)
            .price(this.price)
            .name(this.name)
            .stock(this.stock - quantity) // 변경된 값 적용
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now()) // 변경 시간 업데이트
            .deleteAt(this.deleteAt)
            .build();
    }

    /**
     * 삭제 상태로 변경 후 새로운 Product 인스턴스를 반환 (불변성 유지)
     */
    public Product remove() {
        return Product.builder()
            .id(this.id)
            .seller(this.seller)
            .category(this.category)
            .brand(this.brand)
            .price(this.price)
            .name(this.name)
            .stock(this.stock)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .deleteAt(LocalDateTime.now()) // 삭제 시간 업데이트
            .build();
    }
}
