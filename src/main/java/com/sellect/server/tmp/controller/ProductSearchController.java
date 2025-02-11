package com.sellect.server.tmp.controller;

import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.tmp.dto.response.ProductSearchResponse;
import com.sellect.server.tmp.entity.Product;
import com.sellect.server.tmp.mapper.ProductSearchMapper;
import com.sellect.server.tmp.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductSearchMapper productSearchMapper;

    // TODO: 상품명을 통한 검색인지, 카테고리를 통한 검색인지 구분해야 함
    @GetMapping
    public ApiResponse<List<ProductSearchResponse>> searchByKeyword(@RequestParam String keyword) {

        List<Product> productsByName = productSearchService.searchByProductName(keyword);
        List<Product> productsByCategory = productSearchService.searchByCategory(keyword);

        List<Product> searchResult = new ArrayList<>();
        searchResult.addAll(productsByName);
        searchResult.addAll(productsByCategory);

        return ApiResponse.ok(searchResult.stream()
                .map(productSearchMapper::toProductSearchResponse)
                .toList()
        );
    }

}
