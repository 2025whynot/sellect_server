package com.sellect.server.search.service;

import com.sellect.server.brand.repository.BrandRepository;
import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
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
        Long categoryId = categoryRepository.findByName(keyword).getId();
        return productRepository.findByCategoryId(categoryId);
    }

    private List<Product> searchByBrand(String keyword) {
        Long brandId = brandRepository.findByName(keyword).getId();
        return productRepository.findByBrandId(brandId);
    }

}
