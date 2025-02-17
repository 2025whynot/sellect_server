package com.sellect.server.search.event;

import com.sellect.server.search.domain.SearchLog;
import com.sellect.server.search.repository.SearchLogEntity;
import com.sellect.server.search.repository.SearchLogJpaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchLogEventListener {

    // todo: JPA 의존성 끊기??! SearchLogRepository 를 통해서 .
    private final SearchLogJpaRepository searchLogJpaRepository;

    @EventListener
    public void handleSearchLogEvent(SearchLogEvent event) {

        // 회원인지 비회원인지 식별
        String userIdentifier = event.getUserIdentifier();
        // IP 확인
        String ipAddress = event.getRequest().getRemoteAddr();

        // 이벤트 객체 → 도메인 객체 변환
        SearchLog searchLog = SearchLog.builder()
            .searchKeyword(event.getSearchKeyword())
            .categoryId(event.getCategoryId())
            .brandId(event.getBrandId())
            .userIdentifier(userIdentifier)
            .ipAddress(ipAddress)
            .timestamp(LocalDateTime.now())
            .resultCount(event.getResultCount())
            .filterApplied(event.isFilterApplied())
            .isInitialSearch(event.isInitialSearch())
            .build();

        // 도메인 객체 → JPA 엔티티 변환 후 저장
        searchLogJpaRepository.save(SearchLogEntity.from(searchLog));
    }
}
