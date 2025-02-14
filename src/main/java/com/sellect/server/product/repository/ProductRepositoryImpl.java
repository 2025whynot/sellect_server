package com.sellect.server.product.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductSearchCondition;
import com.sellect.server.product.domain.ProductSortType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final JPAQueryFactory queryFactory;

    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    @Override
    public List<Product> search(ProductSearchCondition condition, int page, int size,
        ProductSortType sortType) {

//        // 0. 필터링을 위한 builder 생성
//        BooleanBuilder builder = new BooleanBuilder();
//
////        if (StringUtils.hasText(condition.getKeyword())) {
////            builder.and(productEntity.name.containsIgnoreCase(condition.getKeyword()));
////        }
//        // 1. BooleanBuilder를 활용한 필터링 (고정 필드만 검색)
//        if (condition.getCategoryId() != null) {
//            builder.and(productEntity.categoryId.eq(condition.getCategoryId()));
//        }
//        if (condition.getBrandId() != null) {
//            builder.and(productEntity.brandId.eq(condition.getBrandId()));
//        }
//        if (condition.getMinPrice() != null) {
//            builder.and(productEntity.price.goe(condition.getMinPrice()));
//        }
//        if (condition.getMaxPrice() != null) {
//            builder.and(productEntity.price.loe(condition.getMaxPrice()));
//        }
//
//        // 2. 정렬 방식 결정
//        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(productEntity, sortType);
//
//        // 4. 기존 정렬 방식 적용 및 페이지네이션
//        List<ProductEntity> fetch = queryFactory
//            .selectFrom(productEntity)
//            .where(builder)
//            .orderBy(orderSpecifier)
//            .offset((long) page * size) // 페이지네이션 적용
//            .limit(size) // 조회할 데이터 개수 설정
//            .fetch();
//
//        return fetch.stream().map(
//            ProductEntity::toModel
//        ).toList();
        return null;
    }

    private OrderSpecifier<?> getOrderSpecifier(QProductEntity product, ProductSortType sortType) {
        return switch (sortType) {
            case REVIEWS ->
                // todo : 상품 리뷰 개수 순
                null; // 좋아요 & 리뷰 정렬은 별도 쿼리에서 처리
            case PRICE_ASC -> product.price.asc();
            case PRICE_DESC -> product.price.desc();
            default -> product.createdAt.desc();
        };
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        List<ProductEntity> savedEntities = productJpaRepository.saveAll(
            products.stream().map(ProductEntity::from).toList()
        );
        return savedEntities.stream().map(ProductEntity::toModel).toList();
    }

    @Override
    public boolean isDuplicateProduct(Long sellerId, String name) {
        return productJpaRepository.existsBySellerEntityIdAndNameAndDeleteAtIsNull(sellerId, name);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findByIdAndDeleteAt(productId, null)
            .map(ProductEntity::toModel);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(ProductEntity.from(product)).toModel();
    }

    @Override
    public Page<Product> findContainingName(String keyword, Pageable pageable) {
        return productJpaRepository.findContainingName(keyword, pageable)
            .map(ProductEntity::toModel);
    }

    @Override
    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        return productJpaRepository.findByCategoryEntityId(categoryId, pageable)
            .map(ProductEntity::toModel);
    }

    @Override
    public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
        return productJpaRepository.findByBrandEntityId(brandId, pageable)
            .map(ProductEntity::toModel);
    }

    @Override
    public Page<Product> findByIdIn(List<Long> ids, Pageable pageable) {
        return productJpaRepository.findByIdIn(ids, pageable)
            .map(ProductEntity::toModel);
    }
}
