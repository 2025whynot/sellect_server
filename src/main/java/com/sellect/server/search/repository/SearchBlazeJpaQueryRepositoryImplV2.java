package com.sellect.server.search.repository;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory;
import com.blazebit.persistence.querydsl.SetExpression;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.sellect.server.search.controller.response.SearchResponse;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import com.sellect.server.search.repository.jpa.MatchedProduct;
import com.sellect.server.search.repository.jpa.QMatchedProduct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.sellect.server.brand.repository.QBrandEntity.brandEntity;
import static com.sellect.server.category.repository.QCategoryEntity.categoryEntity;
import static com.sellect.server.product.repository.QProductEntity.productEntity;
import static com.sellect.server.product.repository.QProductImageEntity.productImageEntity;

@Repository
@RequiredArgsConstructor
public class SearchBlazeJpaQueryRepositoryImplV2 implements SearchRepository {

    private final BlazeJPAQueryFactory queryFactory;
    private final CriteriaBuilderFactory criteriaBuilderFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private BooleanExpression contains(StringPath target, String searchWord) {
        if (searchWord == null || searchWord.isBlank()) {
            return null;
        }
        return Expressions.numberTemplate(Double.class, "function('match_against', {0}, {1})",
            target, searchWord).gt(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchResponse> searchTotal(SearchCondition condition, int page, int size,
        SearchSortType sortType) {
        BooleanBuilder filterBuilder = new BooleanBuilder();

        if (condition.getBrandId() != null) {
            filterBuilder.and(productEntity.brandEntity.id.eq(condition.getBrandId()));
        }
        if (condition.getCategoryId() != null) {
            List<Long> categoryIds = queryFactory
                .select(categoryEntity.id)
                .from(categoryEntity)
                .where(categoryEntity.id.eq(condition.getCategoryId())
                    .or(categoryEntity.parentId.eq(condition.getCategoryId())))
                .fetch();
            filterBuilder.and(productEntity.categoryEntity.id.in(categoryIds));
        }
        if (condition.getMinPrice() != null) {
            filterBuilder.and(productEntity.price.goe(condition.getMinPrice()));
        }
        if (condition.getMaxPrice() != null) {
            filterBuilder.and(productEntity.price.loe(condition.getMaxPrice()));
        }

        String keyword = condition.getKeyword();
        BooleanBuilder keywordBuilder = new BooleanBuilder();
        if (keyword != null && !keyword.isBlank()) {
            keywordBuilder.or(contains(productEntity.name, keyword));
            keywordBuilder.or(contains(brandEntity.name, keyword));
            keywordBuilder.or(contains(categoryEntity.name, keyword));
        } else {
            keywordBuilder.and(productEntity.id.isNotNull());
        }

        // 각 UNION 쿼리에 독립적인 BooleanBuilder
        BooleanBuilder productNameFilter = new BooleanBuilder();
        productNameFilter.and(productEntity.deleteAt.isNull())
            .and(contains(productEntity.name, keyword));

        BooleanBuilder brandNameFilter = new BooleanBuilder();
        brandNameFilter.and(productEntity.deleteAt.isNull())
            .and(contains(brandEntity.name, keyword));

        BooleanBuilder categoryNameFilter = new BooleanBuilder();
        categoryNameFilter.and(productEntity.deleteAt.isNull())
            .and(contains(categoryEntity.name, keyword));

        QMatchedProduct mp = QMatchedProduct.matchedProduct;
        BlazeJPAQuery<MatchedProduct> unionQuery = new BlazeJPAQuery<>(entityManager, criteriaBuilderFactory);
        SetExpression<MatchedProduct> matchedProducts = unionQuery.union(
            new BlazeJPAQuery<MatchedProduct>(entityManager, criteriaBuilderFactory)
                .from(productEntity)
                .bind(mp.id, productEntity.id)
                .bind(mp.name, productEntity.name)
                .bind(mp.price, productEntity.price)
                .bind(mp.createdAt, productEntity.createdAt)
                .bind(mp.brandId, productEntity.brandEntity.id)
                .bind(mp.priority, new CaseBuilder()
                    .when(contains(productEntity.name, keyword)).then(1)
                    .otherwise(4))
                .where(productNameFilter),

            new BlazeJPAQuery<MatchedProduct>(entityManager, criteriaBuilderFactory)
                .from(brandEntity)
                .innerJoin(productEntity)
                .on(productEntity.brandEntity.id.eq(brandEntity.id)
                    .and(productEntity.deleteAt.isNull()))
                .bind(mp.id, productEntity.id)
                .bind(mp.name, productEntity.name)
                .bind(mp.price, productEntity.price)
                .bind(mp.createdAt, productEntity.createdAt)
                .bind(mp.brandId, productEntity.brandEntity.id)
                .bind(mp.priority, Expressions.numberTemplate(Integer.class, "2"))
                .where(brandNameFilter),

            new BlazeJPAQuery<MatchedProduct>(entityManager, criteriaBuilderFactory)
                .from(categoryEntity)
                .innerJoin(productEntity)
                .on(productEntity.categoryEntity.id.eq(categoryEntity.id)
                    .and(productEntity.deleteAt.isNull()))
                .bind(mp.id, productEntity.id)
                .bind(mp.name, productEntity.name)
                .bind(mp.price, productEntity.price)
                .bind(mp.createdAt, productEntity.createdAt)
                .bind(mp.brandId, productEntity.brandEntity.id)
                .bind(mp.priority, Expressions.numberTemplate(Integer.class, "3"))
                .where(categoryNameFilter)
        );

        // 데이터 조회 쿼리
        BlazeJPAQuery<SearchResponse> finalQuery = new BlazeJPAQuery<>(entityManager, criteriaBuilderFactory)
            .with(mp, matchedProducts)
            .select(Projections.constructor(SearchResponse.class,
                brandEntity.name.as("brandName"), // 별칭 추가
                mp.id.stringValue().as("productId"), // 별칭 추가
                productImageEntity.imageUrl,
                mp.name.as("productName"), // 별칭 추가
                mp.price))
            .from(mp)
            .leftJoin(brandEntity).on(brandEntity.id.eq(mp.brandId))
            .leftJoin(productImageEntity)
            .on(productImageEntity.productEntity.id.eq(mp.id)
                .and(productImageEntity.representative.isTrue())
                .and(productImageEntity.deleteAt.isNull()))
            .orderBy(mp.priority.asc(), mp.createdAt.desc())
            .offset((long) page * size)
            .limit(size);

        List<SearchResponse> results = finalQuery.fetch();
        int total = results.size();

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }
}