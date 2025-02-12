package com.sellect.server.search.service;

import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import com.sellect.server.tmp.entity.Brand;
import com.sellect.server.tmp.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSimpleSearchService implements ProductSearchService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchByKeyword(String keyword) {
        if (categoryRepository.existsByName(keyword)) {
            return searchByCategory(keyword);
        }
        if (brandRepository.existsByName(keyword)) {
            return searchByBrand(keyword);
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

    private List<Product> searchByBrand(String keyword) {
        List<Long> brandIds = brandRepository.findContainingName(keyword)
                .stream()
                .map(Brand::getId)
                .toList();
        return productRepository.findByIdIn(brandIds);
    }

}
