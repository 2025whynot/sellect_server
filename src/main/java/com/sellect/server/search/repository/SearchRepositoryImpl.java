package com.sellect.server.search.repository;

import static com.sellect.server.brand.repository.QBrandEntity.brandEntity;
import static com.sellect.server.category.repository.QCategoryEntity.categoryEntity;
import static com.sellect.server.product.repository.QProductEntity.productEntity;
import static com.sellect.server.product.repository.QProductImageEntity.productImageEntity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sellect.server.search.controller.response.SearchResponse;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<SearchResponse> searchTotal(SearchCondition condition, int page, int size,
        SearchSortType sortType) {

        // 0. 필터링을 위한 BooleanBuilder
        BooleanBuilder builder = new BooleanBuilder();

//        // 상품 검색만 할 경우
//        // 0. 동적 검색을 위한 PathBuilder 사용
//        PathBuilder<Product> entityPath = new PathBuilder<>(Product.class, "product");
//        // 1. 상품명 검색(keyword 사용자가 입력한 키워드 검색 - 동적 검색 적용)
//        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
//            builder.and(entityPath.getString("name").containsIgnoreCase(condition.getKeyword()));
//        }

        // 1. 검색 키워드 적용 (카테고리명 → 브랜드명 → 상품명 순서)
        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
            BooleanBuilder keywordBuilder = new BooleanBuilder();
            keywordBuilder.or(productEntity.categoryEntity.name.containsIgnoreCase(
                condition.getKeyword())); // 카테고리명 검색
            keywordBuilder.or(productEntity.brandEntity.name.containsIgnoreCase(
                condition.getKeyword())); // 브랜드명 검색
            keywordBuilder.or(
                productEntity.name.containsIgnoreCase(condition.getKeyword())); // 상품명 검색
            builder.and(keywordBuilder);
        }

        // 상품명 조회 시에는 제거할 것
        // 검색 우선순위 적용: 상품명 → 브랜드명 → 카테고리명
        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
            BooleanBuilder keywordBuilder = new BooleanBuilder();

            keywordBuilder.or(
                productEntity.name.containsIgnoreCase(condition.getKeyword())); // 상품명 검색
            keywordBuilder.or(productEntity.brandEntity.name.containsIgnoreCase(
                condition.getKeyword())); // 브랜드명 검색
            keywordBuilder.or(productEntity.categoryEntity.name.containsIgnoreCase(
                condition.getKeyword())); // 카테고리명 검색

            builder.and(keywordBuilder);
        }

        // 2-1. 브랜드 필터링 (선택한 브랜드 ID)
        if (condition.getBrandId() != null) {
            builder.and(productEntity.brandEntity.id.eq(condition.getBrandId()));
        }
        // 2-2. 카테고리 필터링 (선택한 카테고리 ID)
        if (condition.getCategoryId() != null) {
            builder.and(productEntity.categoryEntity.id.eq(condition.getCategoryId()));
        }
        // 2-3. 가격 필터링
        if (condition.getMinPrice() != null) {
            builder.and(productEntity.price.goe(condition.getMinPrice()));
        }
        if (condition.getMaxPrice() != null) {
            builder.and(productEntity.price.loe(condition.getMaxPrice()));
        }

        // 3. 대표 이미지 URL 서브 쿼리
        JPQLQuery<String> imageUrlSubQuery = JPAExpressions
            .select(productImageEntity.imageUrl)
            .from(productImageEntity)
            .where(productImageEntity.productEntity.id.eq(productEntity.id)
                .and(productImageEntity.representative.isTrue()) // 대표 이미지인지 체크
                .and(productImageEntity.deleteAt.isNull())) // 삭제되지 않은 이미지인지 체크
            .limit(1);

        // 4. 검색 우선순위 정렬 (카테고리명 → 브랜드명 → 상품명)
        OrderSpecifier<Integer> searchPriority = new CaseBuilder()
            .when(productEntity.categoryEntity.name.containsIgnoreCase(condition.getKeyword())).then(1) // 카테고리명 검색
            .when(productEntity.brandEntity.name.containsIgnoreCase(condition.getKeyword())).then(2) // 브랜드명 검색
            .when(productEntity.name.containsIgnoreCase(condition.getKeyword())).then(3) // 상품명 검색
            .otherwise(4) // 기본값
            .asc();

        // 5. 정렬 기준 설정 (리뷰 평점순의 제외)
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(sortType);

        // 6. 최종 QueryDSL 검색 실행 (페이징 처리)
        List<SearchResponse> results = queryFactory
            .select(Projections.constructor(
                SearchResponse.class,
                productEntity.brandEntity.name,
                productEntity.id.stringValue(),
                imageUrlSubQuery,// 대표 이미지 서브 쿼리
                productEntity.name,
                productEntity.price
            ))
            .from(productEntity)
            .leftJoin(productEntity.brandEntity, brandEntity)
            .leftJoin(productEntity.categoryEntity, categoryEntity)
            .where(builder.and(productEntity.deleteAt.isNull()))
            .orderBy(searchPriority, orderSpecifier) // 상품명만 검색할 경우 searchPriority 제거
            .offset((long) page * size)
            .limit(size)
            .fetch();

        // 7. 전체 검색 결과 개수
        Long total = Objects.requireNonNullElse(queryFactory
            .select(productEntity.count())
            .from(productEntity)
            .where(builder.and(productEntity.deleteAt.isNull()))
            .fetchOne(), 0L);

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    private OrderSpecifier<?> getOrderSpecifier(SearchSortType searchSortType) {

        return switch (searchSortType) {
            case PRICE_ASC -> productEntity.price.asc();
            case PRICE_DESC -> productEntity.price.desc();
            default -> productEntity.createdAt.desc();
        };
    }

}