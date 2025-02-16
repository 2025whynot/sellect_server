package com.sellect.server.search.service;

import java.util.List;

public interface AutoCompleteService {

    List<String> getAutoCompleteResult(String prefix);

}
