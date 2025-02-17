package com.sellect.server.search.application;

import com.sellect.server.search.domain.AutoCompleteKeyword;
import com.sellect.server.search.repository.AutoCompleteKeywordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoCompleteSimpleService implements AutoCompleteService {

    private final AutoCompleteKeywordRepository autoCompleteKeywordRepository;

    @Override
    public List<String> getAutoCompleteResult(String query) {
        return autoCompleteKeywordRepository.findTopKeywordsStartingWith(query).stream()
            .map(AutoCompleteKeyword::getKeyword)
            .map(String::valueOf)
            .toList();
    }

}
