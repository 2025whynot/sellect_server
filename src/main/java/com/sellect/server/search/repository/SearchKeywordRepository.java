package com.sellect.server.search.repository;

import com.sellect.server.search.domain.SearchKeyword;
import java.util.List;

public interface SearchKeywordRepository {

    List<SearchKeyword> findTopKeywordsStartingWith(String query);

}
