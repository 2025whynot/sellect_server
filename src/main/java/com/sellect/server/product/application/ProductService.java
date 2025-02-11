package com.sellect.server.product.application;

import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductRegisterFailureResponse;
import com.sellect.server.product.controller.response.ProductRegisterResponse;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // todo : 이미지 고려 안 함 아직 S3 없음
    @Transactional
    public ProductRegisterResponse registerMultiple(Long sellerId, List<ProductRegisterRequest> requests) {
        List<Product> successProducts = new ArrayList<>();
        List<ProductRegisterFailureResponse> failedProducts = new ArrayList<>();

        // 요청 내 상품명 중복 검증을 위한 Set
        Set<String> requestProductNames = new HashSet<>();

        for (ProductRegisterRequest request : requests) {
            try {
                // 요청 내 상품 기준 중복 검사
                if (!requestProductNames.add(request.name())) {
                    failedProducts.add(
                        ProductRegisterFailureResponse.from(request.name(), "요청 내 중복된 상품명")
                    );
                    continue;
                }

                // 존재하지 않는 카테고리 체크
                if (!categoryRepository.isExistCategory(request.categoryId(), null)) {
                    System.out.println(
                        categoryRepository.isExistCategory(request.categoryId(), null));
                    failedProducts.add(
                        ProductRegisterFailureResponse.from(request.name(), "존재하지 않는 카테고리"));
                    continue;
                }

                // 등록된 상품 기준 중복 검사 (sellerId, productName 기준)
                if (productRepository.isDuplicateProduct(sellerId, request.name(), null)) {
                    failedProducts.add(
                        ProductRegisterFailureResponse.from(request.name(), "중복 상품"));
                    continue;
                }

                // todo : 상품당 이미지는 필수


                successProducts.add(Product.register(
                    sellerId,
                    request.categoryId(),
                    request.brandId(),
                    request.getPriceAsBigDecimal(), // String -> BigDecimal 변환
                    request.name(),
                    request.stock()
                ));

            } catch (Exception e) {
                // 알 수 없는 예외 발생 시 실패 처리
                failedProducts.add(
                    ProductRegisterFailureResponse.from(request.name(), "알 수 없는 오류 발생"));
            }
        }

        // 기획 : 실패한 게 하나도 없을 때에만 등록이 가능
        if (!failedProducts.isEmpty()) {
            productRepository.saveAll(successProducts);
        }

        // 성공 및 실패 리스트 반환
        return ProductRegisterResponse.from(successProducts, failedProducts);
    }
}
