package com.sellect.server.search.repository;

import com.sellect.server.search.domain.SearchKeyword;
import com.sellect.server.search.mapper.SearchMapper;
import com.sellect.server.search.repository.jpa.SearchKeywordEntity;
import com.sellect.server.search.repository.jpa.SearchKeywordJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchKeywordRepositoryImpl implements SearchKeywordRepository {

    private static final int MAX_LIMIT = 10;

    private final SearchKeywordJpaRepository searchKeywordJpaRepository;
    private final SearchMapper searchMapper;

    @Override
    public List<SearchKeyword> findTopKeywordsStartingWith(String query) {
        List<SearchKeywordEntity> findKeywords = searchKeywordJpaRepository
            .findTopByKeywordStartingWith(query, MAX_LIMIT);
        return findKeywords.stream()
            .map(searchMapper::toModel)
            .toList();
    }
}
