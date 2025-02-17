package com.sellect.server.product.domain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
@Builder
public class SearchLogEvent {
    private String searchKeyword;
    private Long categoryId;
    private Long brandId;
    private int resultCount;
    private boolean filterApplied;
    private String userIdentifier;
    private boolean isInitialSearch;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public static SearchLogEvent publish(String searchKeyword, Long categoryId, Long brandId, int resultCount,
        boolean filterApplied, String userIdentifier, boolean isInitialSearch, HttpServletRequest request, HttpServletResponse response) {
        return SearchLogEvent.builder()
            .searchKeyword(searchKeyword)
            .categoryId(categoryId)
            .brandId(brandId)
            .resultCount(resultCount)
            .filterApplied(filterApplied)
            .userIdentifier(userIdentifier)
            .isInitialSearch(isInitialSearch)
            .request(request)
            .response(response)
            .build();
    }
}
