package com.sellect.server.search.application;

import java.util.List;

public interface AutoCompleteService {

    List<String> getAutoCompleteResult(String prefix);

}
