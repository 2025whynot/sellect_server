package com.sellect.server.search.repository;

import com.sellect.server.product.domain.Product;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import java.util.List;

public interface SearchRepository {

    // 상품 조회 (동적 쿼리)
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    List<Product> searchTotal(
        SearchCondition condition, int page, int size, SearchSortType sortType);

}
