package com.sellect.server.search.domain;

import java.time.LocalDateTime;
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
    private final Long frequency;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

}