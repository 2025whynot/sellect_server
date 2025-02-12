package com.sellect.server.tmp.service;

import com.sellect.server.tmp.entity.Category;
import com.sellect.server.tmp.entity.Product;
import com.sellect.server.tmp.repository.CategoryRepository;
import com.sellect.server.tmp.repository.ProductTempRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductTempRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<Product> searchByProductName(String keyword) {
        return productRepository.findByNameLike(keyword);
    }

    public List<Product> searchByCategory(String keyword) {
        List<Long> categoryIds = categoryRepository.findByNameLike(keyword).stream()
                .map(Category::getId)
                .toList();
        return productRepository.findByIdIn(categoryIds);
    }
}
