package com.sellect.server.search.service;

import com.sellect.server.product.domain.Product;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import com.sellect.server.search.event.SearchLogEvent;
import com.sellect.server.search.util.UserIdentifierUtil;
import com.sellect.server.search.repository.SearchRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    // todo: 추후 SearchService 로 옮길 예정
    private final ApplicationEventPublisher eventPublisher;

    // todo: 검색 조회 후에는 이벤트 발생 시켜서 로그 데이터 쌓기
    @Transactional(readOnly = true)
    public List<Product> searchTotal(Long userId, SearchCondition condition, int page, int size,
        SearchSortType sortType, boolean isInitialSearch, HttpServletRequest request, HttpServletResponse response) {

        List<Product> searchProducts = searchRepository.searchTotal(condition, page, size, sortType);

        // 이벤트 발생 (로그 데이터)
        // 0. 유의미한 검색 키워드인지 판별을 위해
        int totalResults = searchProducts.size();
        // 0. 필터를 사용했는지 안했는지 체크 -> 사용한 로그일 경우에는 사용하지 않은 요청과 같은 요청으로 묶이도록 필터링 예정
        boolean isFilterApplied = isFilterUsed(condition);
        // 회원 / 비회원 구분 (NPE 방지)
        String userIdentifier = UserIdentifierUtil.getUserIdentifier(userId, request, response);

        // 1. 이벤트 발생
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

    private boolean isFilterUsed(SearchCondition condition) {
        return condition.getCategoryId() != null || condition.getBrandId() != null;
    }

}
