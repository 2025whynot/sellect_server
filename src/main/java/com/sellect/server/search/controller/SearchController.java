package com.sellect.server.search.controller;

import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.product.domain.Product;
import com.sellect.server.search.controller.response.AutoCompleteResponse;
import com.sellect.server.search.controller.response.ProductSearchResponse;
import com.sellect.server.search.mapper.SearchMapper;
import com.sellect.server.search.application.AutoCompleteService;
import com.sellect.server.search.application.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;
    private final AutoCompleteService autoCompleteService;
    private final SearchMapper searchMapper;

    @GetMapping
    public ApiResponse<Page<ProductSearchResponse>> searchByKeyword(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(value = "sort_prop", defaultValue = "id") String sortProp,
        @RequestParam(value = "sort_type", defaultValue = "desc") String sortType
    ) {
        // TODO: page 사이즈 하드코딩 제거 및 최댓값 설정
        PageRequest pageRequest = PageRequest.of(page, 10,
            Sort.by(Sort.Direction.fromString(sortType), sortProp));
        Page<Product> products = productSearchService.searchByKeyword(keyword, pageRequest);
        return ApiResponse.ok(products.map(searchMapper::toProductSearchResponse));
    }

    @GetMapping("/auto-complete")
    public ApiResponse<AutoCompleteResponse> autoCompleteSearch(
        @RequestParam(value = "q") String query) {
        return ApiResponse.ok(
            new AutoCompleteResponse(autoCompleteService.getAutoCompleteResult(query)));
    }

}
