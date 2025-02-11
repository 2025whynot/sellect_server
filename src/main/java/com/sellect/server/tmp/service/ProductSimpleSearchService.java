package com.sellect.server.tmp.service;

import com.sellect.server.tmp.entity.Category;
import com.sellect.server.tmp.entity.Product;
import com.sellect.server.tmp.repository.CategoryRepository;
import com.sellect.server.tmp.repository.ProductTempRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProductSimpleSearchService implements ProductSearchService {

    private final ProductTempRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchByKeyword(String keyword) {
        if (categoryRepository.existsByName(keyword)) {
            return searchByCategory(keyword);
        }
        return searchByProductName(keyword);
    }

    private List<Product> searchByProductName(String keyword) {
        return productRepository.findContainingName(keyword);
    }

    private List<Product> searchByCategory(String keyword) {
        List<Long> categoryIds = categoryRepository.findContainingName(keyword)
                .stream()
                .map(Category::getId)
                .toList();
        return productRepository.findByIdIn(categoryIds);
    }

}
