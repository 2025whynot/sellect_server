package com.sellect.server.search.controller.response;

import java.util.List;

public record AutoCompleteResponse(
    List<String> keywords
) {

}
