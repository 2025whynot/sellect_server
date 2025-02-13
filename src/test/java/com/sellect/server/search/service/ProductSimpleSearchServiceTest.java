//package com.sellect.server.search.service;
//
//import com.sellect.server.brand.domain.Brand;
//import com.sellect.server.brand.repository.FakeBrandRepository;
//import com.sellect.server.category.domain.Category;
//import com.sellect.server.category.repository.FakeCategoryRepository;
//import com.sellect.server.product.domain.Product;
//import com.sellect.server.product.repository.FakeProductRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//
//import java.math.BigDecimal;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class ProductSimpleSearchServiceTest {
//
//    private final FakeProductRepository mockProductRepository = new FakeProductRepository();
//    private final FakeCategoryRepository mockCategoryRepository = new FakeCategoryRepository();
//    private final FakeBrandRepository mockBrandRepository = new FakeBrandRepository();
//    private final ProductSimpleSearchService productSearchService = new ProductSimpleSearchService(
//        mockProductRepository,
//        mockCategoryRepository,
//        mockBrandRepository);
//
//    @BeforeEach
//    void setUp() {
//        mockProductRepository.clear();
//        mockCategoryRepository.clear();
//        mockBrandRepository.clear();
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"test", "product"})
//    @DisplayName("검색 키워드가 상품명에 포함되면 해당 상품을 반환한다.")
//    void searchByKeyword_shouldReturnMatchingProducts(String keyword) {
//        // given
//        Product testProduct1 = Product.builder()
//            .id(1L)
//            .name("test product 1")
//            .price(BigDecimal.valueOf(1000))
//            .stock(100)
//            .build();
//        Product testProduct2 = Product.builder()
//            .id(2L)
//            .name("test product 2")
//            .price(BigDecimal.valueOf(2000))
//            .stock(100)
//            .build();
//
//        mockProductRepository.save(testProduct1);
//        mockProductRepository.save(testProduct2);
//
//        // when
//        Page<Product> searchResults = productSearchService.searchByKeyword(
//            keyword, PageRequest.of(0, 10));
//
//        // then
//        assertThat(searchResults.getTotalElements()).isEqualTo(2);
//        assertThat(searchResults.getContent()).contains(testProduct1, testProduct2);
//    }
//
//    @Test
//    @DisplayName("키워드가 카테고리 이름과 일치하면 해당 카테고리의 상품을 반환한다.")
//    void searchByCategory_shouldReturnProductsInCategory() {
//        // given
//        Category category = Category.builder().id(1L).name("Electronics").build();
//        mockCategoryRepository.save(category);
//
//        Product product = Product.builder()
//            .id(1L)
//            .name("Smartphone")
//            .categoryId(category.getId())
//            .price(BigDecimal.valueOf(999))
//            .stock(50)
//            .build();
//        mockProductRepository.save(product);
//
//        // when
//        Page<Product> searchResults = productSearchService.searchByKeyword(
//            "Electronics", PageRequest.of(0, 10));
//
//        // then
//        assertThat(searchResults.getTotalElements()).isEqualTo(1);
//        assertThat(searchResults.getContent()).contains(product);
//    }
//
//    @Test
//    @DisplayName("키워드가 브랜드 이름과 일치하면 해당 브랜드의 상품을 반환한다.")
//    void searchByBrand_shouldReturnProductsFromBrand() {
//        // given
//        Brand brand = Brand.builder().id(1L).name("Apple").build();
//        mockBrandRepository.save(brand);
//
//        Product product = Product.builder()
//            .id(1L)
//            .name("iPhone 13")
//            .brandId(brand.getId())
//            .price(BigDecimal.valueOf(1200))
//            .stock(30)
//            .build();
//        mockProductRepository.save(product);
//
//        // when
//        Page<Product> searchResults = productSearchService.searchByKeyword(
//            "Apple", PageRequest.of(0, 10));
//
//        // then
//        assertThat(searchResults.getTotalElements()).isEqualTo(1);
//        assertThat(searchResults.getContent()).contains(product);
//    }
//}