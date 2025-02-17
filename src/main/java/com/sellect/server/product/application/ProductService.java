package com.sellect.server.product.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.BrandRepository;
import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.product.controller.request.ProductModifyRequest;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductModifyResponse;
import com.sellect.server.product.controller.response.ProductRegisterFailureResponse;
import com.sellect.server.product.controller.response.ProductRegisterResponse;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductSearchCondition;
import com.sellect.server.product.domain.ProductSortType;
import com.sellect.server.product.domain.SearchLogEvent;
import com.sellect.server.product.repository.ProductRepository;
import com.sellect.server.product.util.UserIdentifierUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    // todo: 추후 SearchService 로 옮길 예정
    private final ApplicationEventPublisher eventPublisher;

    // todo : 이미지 고려 안 함 아직 S3 없음
    @Transactional
    public ProductRegisterResponse registerMultiple(User seller,
        List<ProductRegisterRequest> requests) {
        List<Product> successProducts = new ArrayList<>();
        List<ProductRegisterFailureResponse> failedProducts = new ArrayList<>();

        // 요청 내 상품명 중복 검증을 위한 Set
        Set<String> requestProductNames = new HashSet<>();

        for (ProductRegisterRequest request : requests) {
            // 요청 내 상품 기준 중복 검사
            if (!requestProductNames.add(request.name())) {
                failedProducts.add(
                    ProductRegisterFailureResponse.from(request.name(), "요청 내 중복된 상품명")
                );
                continue;
            }

            Optional<Category> optionalCategory = categoryRepository.findById(request.categoryId());
            // 존재하지 않는 카테고리 체크
            if (optionalCategory.isEmpty()) {
                failedProducts.add(
                    ProductRegisterFailureResponse.from(request.name(), "존재하지 않는 카테고리"));
                continue;
            }

            // 존재하지 않는 브랜드 체크
            Optional<Brand> optionalBrand = brandRepository.findById(request.brandId());
            if (optionalBrand.isEmpty()) {
                failedProducts.add(
                    ProductRegisterFailureResponse.from(request.name(), "존재하지 않는 브랜드"));
                continue;
            }

            // 등록된 상품 기준 중복 검사 (sellerId, productName 기준)
            if (productRepository.isDuplicateProduct(seller.getId(), request.name())) {
                failedProducts.add(
                    ProductRegisterFailureResponse.from(request.name(), "중복 상품"));
                continue;
            }

            // todo : 상품당 이미지는 필수

            successProducts.add(Product.register(
                seller,
                optionalCategory.get(),
                optionalBrand.get(),
                request.getPriceAsBigDecimal(), // String -> BigDecimal 변환
                request.name(),
                request.stock()
            ));

        }

        // 기획 : 실패한 게 하나도 없을 때에만 등록이 가능
        if (failedProducts.isEmpty()) {
            productRepository.saveAll(successProducts);
        }

        // 성공 및 실패 리스트 반환
        return ProductRegisterResponse.from(successProducts, failedProducts);
    }

    @Transactional
    public ProductModifyResponse modify(Long sellerId, Long productId,
        ProductModifyRequest request) {

        // 수정할 상품이 존재하는지 확인
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품이 존제하지 않습니다."));

        // 유저의 상품이 맞는지 확인
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("상품을 수정할 권한이 없습니다.");
        }

        // 수정할 값이 존재할 경우만 수정
        Product modifiedProduct = product.modify(
            Optional.ofNullable(request.getPriceAsBigDecimal()).orElse(product.getPrice()),
            Optional.ofNullable(request.name()).orElse(product.getName()),
            Optional.ofNullable(request.stock()).orElse(product.getStock())
        );

        productRepository.save(modifiedProduct);

        return ProductModifyResponse.from(modifiedProduct);
    }

    @Transactional
    public void remove(Long sellerId, Long productId) {

        // 삭제할 상품이 존재하는지 확인
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품이 존제하지 않습니다."));

        // 유저의 상품이 맞는지 확인
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("상품을 수정할 권한이 없습니다.");
        }

        productRepository.save(product.remove());
    }

    // todo: 검색 조회 후에는 이벤트 발생 시켜서 로그 데이터 쌓기
    @Transactional(readOnly = true)
    public List<Product> search(Long userId, ProductSearchCondition condition, int page, int size,
        ProductSortType sortType, boolean isInitialSearch, HttpServletRequest request, HttpServletResponse response) {

        List<Product> searchProducts = productRepository.search(condition, page, size, sortType);

        // 이벤트 발생 (로그 데이터)
        // 0. 유의미한 검색 키워드인지 판별을 위해
        int totalResults = searchProducts.size();
        // 0. 필터를 사용했는지 안했는지 체크 -> 사용한 로그일 경우에는 사용하지 않은 요청과 같은 요청으로 묶이도록 필터링 예정
        boolean isFilterApplied = isFilterUsed(condition);
        // 회원 / 비회원 구분 (NPE 방지)
        String userIdentifier = UserIdentifierUtil.getUserIdentifier(userId, request, response);

        // 1. 이벤트 발생
        eventPublisher.publishEvent(SearchLogEvent.publish(
            condition.getKeyword(),
            condition.getCategoryId(),
            condition.getBrandId(),
            totalResults,
            isFilterApplied,
            userIdentifier,
            isInitialSearch,
            request,
            response
        ));

        return searchProducts;
    }

    private boolean isFilterUsed(ProductSearchCondition condition) {
        return condition.getCategoryId() != null || condition.getBrandId() != null;
    }

}
