package com.sellect.server.search.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.product.domain.Product;
import com.sellect.server.search.domain.SearchCondition;
import com.sellect.server.search.domain.SearchSortType;
import com.sellect.server.search.controller.response.AutoCompleteResponse;
import com.sellect.server.search.service.AutoCompleteService;
import com.sellect.server.search.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
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
    private final SearchService searchService;

    @GetMapping("/auto-complete")
    public ApiResponse<AutoCompleteResponse> autoCompleteSearch(
        @RequestParam(value = "q") String query) {
        return ApiResponse.ok(
            new AutoCompleteResponse(autoCompleteService.getAutoCompleteResult(query)));
    }

    /*
     * 상품 조회 API
     * condition 을 통해 확인
     * */
    @GetMapping("/products/search")
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    public List<Product> searchTotal(
        @AuthUser User user,
        @RequestParam String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "LATEST") SearchSortType sortType,
        @RequestParam(defaultValue = "false") boolean isInitialSearch,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        SearchCondition condition = SearchCondition
            .builder()
            .keyword(keyword)
            .categoryId(categoryId)
            .brandId(brandId)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .build();

        Long userId = (user != null) ? user.getId() : null;
        // todo : SearchResponse
        return searchService.searchTotal(userId, condition, page, size, sortType, isInitialSearch, request, response);
    }
}
