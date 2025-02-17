package com.sellect.server.search.application;

import com.sellect.server.search.controller.response.SearchResponse;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import com.sellect.server.search.event.SearchLogEvent;
import com.sellect.server.search.repository.SearchRepository;
import com.sellect.server.search.util.UserIdentifierUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<SearchResponse> searchTotal(Long userId, SearchCondition condition, int page,
        int size,
        SearchSortType sortType, boolean isInitialSearch, HttpServletRequest request,
        HttpServletResponse response) {

        // 0. 상품 검색 실행
        Page<SearchResponse> searchProducts = searchRepository.searchTotal(condition, page, size,
            sortType);

        // 이벤트 발생 (로그 데이터)
        // 1. 유의미한 검색 키워드인지 판별을 위해
        int totalResults = (int) searchProducts.getTotalElements();

        // 2. 필터를 사용했는지 안했는지 체크 -> 사용한 로그일 경우에는 사용하지 않은 요청과 같은 요청으로 묶이도록 필터링 예정
        boolean isFilterApplied = isFilterUsed(condition);

        // 3.회원 / 비회원 구분 (NPE 방지)
        String userIdentifier = UserIdentifierUtil.getUserIdentifier(userId, request, response);

        // 4. 검색 로그 이벤트 발생
        eventPublisher.publishEvent(SearchLogEvent.publish(
            condition.getKeyword(),
            condition.getCategoryId(),
            condition.getBrandId(),
            totalResults,
            isFilterApplied,
            userIdentifier,
            isInitialSearch,
            request,
            response
        ));

        return searchProducts;
    }

    /**
     * 필터 사용 여부 체크 (카테고리 또는 브랜드 필터 적용 여부)
     */
    private boolean isFilterUsed(SearchCondition condition) {
        return condition.getCategoryId() != null || condition.getBrandId() != null;
    }

}
