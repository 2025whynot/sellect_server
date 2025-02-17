package com.sellect.server.search.repository;

import static com.sellect.server.product.repository.QProductEntity.productEntity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import com.sellect.server.product.repository.ProductEntity;
import com.sellect.server.product.repository.QProductEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> searchTotal(SearchCondition condition, int page, int size,
        SearchSortType sortType) {

        // 0. 필터링을 위한 builder 생성
        BooleanBuilder builder = new BooleanBuilder();

//        if (StringUtils.hasText(condition.getKeyword())) {
//            builder.and(productEntity.name.containsIgnoreCase(condition.getKeyword()));
//        }
        // 1. BooleanBuilder를 활용한 필터링 (고정 필드만 검색)
        if (condition.getCategoryId() != null) {
            builder.and(productEntity.categoryEntity.id.eq(condition.getCategoryId()));
        }
        if (condition.getBrandId() != null) {
            builder.and(productEntity.brandEntity.id.eq(condition.getBrandId()));
        }
        if (condition.getMinPrice() != null) {
            builder.and(productEntity.price.goe(condition.getMinPrice()));
        }
        if (condition.getMaxPrice() != null) {
            builder.and(productEntity.price.loe(condition.getMaxPrice()));
        }

        // 2. 정렬 방식 결정
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(productEntity, sortType);

        // 4. 기존 정렬 방식 적용 및 페이지네이션
        List<ProductEntity> fetch = queryFactory
            .selectFrom(productEntity)
            .where(builder)
            .orderBy(orderSpecifier)
            .offset((long) page * size) // 페이지네이션 적용
            .limit(size) // 조회할 데이터 개수 설정
            .fetch();

        return fetch.stream().map(
            ProductEntity::toModel
        ).toList();
    }

    private OrderSpecifier<?> getOrderSpecifier(QProductEntity product, SearchSortType sortType) {
        return switch (sortType) {
            case REVIEWS ->
                // todo : 상품 리뷰 개수 순
                null; // 좋아요 & 리뷰 정렬은 별도 쿼리에서 처리
            case PRICE_ASC -> product.price.asc();
            case PRICE_DESC -> product.price.desc();
            default -> product.createdAt.desc();
        };
    }

}
