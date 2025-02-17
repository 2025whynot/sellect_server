package com.sellect.server.search.repository;

import com.sellect.server.search.domain.AutoCompleteKeyword;
import java.util.List;

public interface AutoCompleteKeywordRepository {

    List<AutoCompleteKeyword> findTopKeywordsStartingWith(String query);

}
