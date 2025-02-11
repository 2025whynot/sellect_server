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

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductSearchMapper productSearchMapper;

    @GetMapping
    public ApiResponse<List<ProductSearchResponse>> searchByKeyword(@RequestParam String keyword) {
        List<Product> products = productSearchService.searchByKeyword(keyword);
        return ApiResponse.ok(products.stream()
                .map(productSearchMapper::toProductSearchResponse)
                .toList()
        );
    }

}
