package com.sellect.server.search.repository;

import com.sellect.server.search.domain.AutoCompleteKeyword;
import com.sellect.server.search.repository.jpa.AutoCompleteKeywordEntity;
import com.sellect.server.search.repository.jpa.AutoCompleteKeywordJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AutoCompleteKeywordRepositoryImpl implements AutoCompleteKeywordRepository {

    private static final int MAX_LIMIT = 10;

    private final AutoCompleteKeywordJpaRepository autoCompleteKeywordJpaRepository;

    @Override
    public List<AutoCompleteKeyword> findTopKeywordsStartingWith(String query) {
        List<AutoCompleteKeywordEntity> findKeywords = autoCompleteKeywordJpaRepository
            .findTopByKeywordStartingWith(query, MAX_LIMIT);
        return findKeywords.stream()
            .map(AutoCompleteKeywordEntity::toModel)
            .toList();
    }
}
