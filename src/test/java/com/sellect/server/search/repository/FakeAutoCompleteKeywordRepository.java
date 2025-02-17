package com.sellect.server.search.repository;

import com.sellect.server.search.domain.AutoCompleteKeyword;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FakeAutoCompleteKeywordRepository implements AutoCompleteKeywordRepository {

    private static final int MAX_LIMIT = 10;

    private final List<AutoCompleteKeyword> data = new ArrayList<>();

    @Override
    public List<AutoCompleteKeyword> findTopKeywordsStartingWith(String query) {
        return data.stream()
            .filter(searchKeyword -> searchKeyword.getKeyword().startsWith(query))
            .sorted(Comparator.comparing(AutoCompleteKeyword::getFrequency).reversed())
            .limit(MAX_LIMIT)
            .toList();
    }

    public void save(AutoCompleteKeyword autoCompleteKeyword) {
        data.add(autoCompleteKeyword);
    }

    public void saveAll(List<AutoCompleteKeyword> autoCompleteKeywords) {
        data.addAll(autoCompleteKeywords);
    }

}
