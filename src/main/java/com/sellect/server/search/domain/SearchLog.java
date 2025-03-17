package com.sellect.server.search.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
public class SearchLog {
    private final Long id;
    private final String keyword;
    private final String userIdentifier;
    private final int resultCount;
    private final boolean filterApplied;
    private final LocalDateTime timestamp;
    private final Long categoryId;
    private final Long brandId;
}
