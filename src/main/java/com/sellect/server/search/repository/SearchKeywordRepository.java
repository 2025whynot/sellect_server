package com.sellect.server.search.repository;

import com.sellect.server.search.domain.SearchKeyword;
import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository {

    List<SearchKeyword> findTopKeywordsStartingWith(String query);

    Optional<SearchKeyword> findByKeyword(String keyword);


}
