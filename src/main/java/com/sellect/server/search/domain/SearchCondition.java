package com.sellect.server.search.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
@Builder
public class SearchCondition {

    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    private final String keyword;
    private final Long categoryId;
    private final Long brandId;
    private final Integer minPrice;
    private final Integer maxPrice;
}
