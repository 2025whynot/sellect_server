package com.sellect.server.search.domain;

import com.sellect.server.search.repository.SearchKeywordEntity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchKeyword {

    private final Long id;
    private final String keyword;
    private final Long searchCount;
    private final SearchKeywordEntity parent;
    private final List<SearchKeywordEntity> children;
    private final List<CachedKeyword> cachedKeywords;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

}