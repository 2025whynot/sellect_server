package com.sellect.server.search.config;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sellect.server.search.repository.SearchRepository;
import com.sellect.server.search.repository.SearchBlazeJpaQueryRepositoryImplV2;
import com.sellect.server.search.repository.SearchRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchRepositoryConfig {

    /*
    * 단순 기능 구현
    * 튜닝 전 단순 QueryDsl 사용
    * */
//    @Bean
//    public SearchRepository searchRepository(JPAQueryFactory queryFactory) {
//        return new SearchRepositoryImpl(queryFactory);
//    }

    /*
    * 성능 최적화
    * 방법 1.쿼리 최적화 및 FullText Scan 사용
    * */
//    @Bean
//    public SearchRepository searchRepository(BlazeJPAQueryFactory queryFactory, CriteriaBuilderFactory criteriaBuilderFactory) {
//        return new SearchBlazeJpaQueryRepositoryImpl(queryFactory, criteriaBuilderFactory);
//    }

    @Bean
    public SearchRepository searchRepository(BlazeJPAQueryFactory queryFactory,
        CriteriaBuilderFactory criteriaBuilderFactory) {
        return new SearchBlazeJpaQueryRepositoryImplV2(queryFactory, criteriaBuilderFactory);
    }
}
