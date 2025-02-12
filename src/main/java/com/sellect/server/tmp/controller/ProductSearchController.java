package com.sellect.server.tmp.controller;

import com.sellect.server.tmp.entity.Product;
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

    // TODO: 상품명을 통한 검색인지, 카테고리를 통한 검색인지 구분해야 함
    @GetMapping
    public List<Product> search(@RequestParam String keyword) {
        List<Product> productsByName = productSearchService.searchByProductName(keyword);
        List<Product> productsByCategory = productSearchService.searchByCategory(keyword);
        List<Product> result = new ArrayList<>();
        result.addAll(productsByName);
        result.addAll(productsByCategory);
        return result;
    }

}
