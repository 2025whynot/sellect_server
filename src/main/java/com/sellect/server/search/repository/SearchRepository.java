package com.sellect.server.search.repository;

import com.sellect.server.search.controller.response.SearchResponse;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import org.springframework.data.domain.Page;

public interface SearchRepository {

    // 상품 조회 (동적 쿼리)
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    Page<SearchResponse> searchTotal(
        SearchCondition condition, int page, int size, SearchSortType sortType);

}
