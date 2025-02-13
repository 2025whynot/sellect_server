package com.sellect.server.search.service;

import com.sellect.server.search.domain.SearchKeyword;
import com.sellect.server.search.repository.SearchKeywordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoCompleteSimpleService implements AutoCompleteService {

    private final SearchKeywordRepository searchKeywordRepository;

    @Override
    public List<String> getAutoCompleteResult(String query) {
        return searchKeywordRepository.findTopKeywordsStartingWith(query).stream()
            .map(SearchKeyword::getKeyword)
            .map(String::valueOf)
            .toList();
    }

}
