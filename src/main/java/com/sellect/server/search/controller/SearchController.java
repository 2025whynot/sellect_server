package com.sellect.server.search.controller;

import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.search.controller.response.AutoCompleteResponse;
import com.sellect.server.search.service.AutoCompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final AutoCompleteService autoCompleteService;


    @GetMapping("/auto-complete")
    public ApiResponse<AutoCompleteResponse> autoCompleteSearch(
        @RequestParam(value = "q") String query) {
        return ApiResponse.ok(
            new AutoCompleteResponse(autoCompleteService.getAutoCompleteResult(query)));
    }

}
