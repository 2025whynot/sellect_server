package com.sellect.server.product.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
@Builder
public class SearchLog {
    private final Long id;
    private final String searchKeyword;
    private final Long categoryId;
    private final Long brandId;
    private final String userIdentifier;
    private final String ipAddress;
    private final LocalDateTime timestamp;
    private final int resultCount;
    private final boolean filterApplied;
    private final boolean isInitialSearch;
}
