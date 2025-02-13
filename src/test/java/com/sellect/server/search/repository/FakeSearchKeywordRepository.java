package com.sellect.server.search.repository;

import com.sellect.server.search.domain.SearchKeyword;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FakeSearchKeywordRepository implements SearchKeywordRepository {

    private static final int MAX_LIMIT = 10;

    private final List<SearchKeyword> data = new ArrayList<>();

    @Override
    public List<SearchKeyword> findTopKeywordsStartingWith(String query) {
        return data.stream()
            .filter(searchKeyword -> searchKeyword.getKeyword().startsWith(query))
            .sorted(Comparator.comparing(SearchKeyword::getFrequency).reversed())
            .limit(MAX_LIMIT)
            .toList();
    }

    public void save(SearchKeyword searchKeyword) {
        data.add(searchKeyword);
    }

    public void saveAll(List<SearchKeyword> searchKeywords) {
        data.addAll(searchKeywords);
    }

}
