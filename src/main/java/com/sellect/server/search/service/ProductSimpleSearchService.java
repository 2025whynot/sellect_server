package com.sellect.server.search.service;

import com.sellect.server.brand.repository.BrandRepository;
import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSimpleSearchService implements ProductSearchService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final BrandRepository brandRepository;

  // TODO: 정렬 기준 -> 신상품순(기본), 가격순, 좋아요순, 후기순 등에 따라 정렬 기능 추가
  @Override
  @Transactional(readOnly = true)
  public Page<Product> searchByKeyword(String keyword, Pageable pageable) {
    if (categoryRepository.existsByName(keyword)) {
      return searchByCategory(keyword, pageable);
    }
    if (brandRepository.existsByName(keyword)) {
      return searchByBrand(keyword, pageable);
    }
    return searchByProductName(keyword, pageable);
  }

  private Page<Product> searchByProductName(String keyword, Pageable pageable) {
    return productRepository.findContainingName(keyword, pageable);
  }

  private Page<Product> searchByCategory(String keyword, Pageable pageable) {
    Long categoryId = categoryRepository.findByName(keyword).getId();
    return productRepository.findByCategoryId(categoryId, pageable);
  }

  private Page<Product> searchByBrand(String keyword, Pageable pageable) {
    Long brandId = brandRepository.findByName(keyword).getId();
    return productRepository.findByBrandId(brandId, pageable);
  }

}
