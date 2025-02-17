package com.sellect.server.search.domain;

public enum SearchSortType {
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    LATEST,   // 최신순
    REVIEWS, // 리뷰 개수 순
    PRICE_ASC,  // 가격 낮은 순
    PRICE_DESC; // 가격 높은 순
}
