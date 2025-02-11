package com.sellect.server.product.controller.application;

import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import java.util.List;
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
    public List<Product> registerMultiple(Long sellerId, List<ProductRegisterRequest> requests) {
        // 존재하지 않는 카테고리 체크
        requests.forEach(request -> {
            categoryRepository.findById(request.categoryId(), null)
                .orElseThrow(() -> new RuntimeException("Not Found Category By Id"));
        });

        // todo : 판매자가 동일한 상품을 등록하지 못하게끔

        requests.forEach(request -> {
            if (productRepository.isDuplicateProduct(sellerId, request.name(), null)) {
                throw new RuntimeException("DUPLICATE_PRODUCT");
            }

                
        });

        // todo : 상품당 이미지는 필수

        // productRepository 에 save 치기
        List<Product> products = requests.stream()
            .map(request -> Product.register(
                sellerId,
                request.categoryId(),
                request.brandId(),
                request.getPriceAsBigDecimal(), // String -> BigDecimal 변환
                request.name(),
                request.stock()
            )).toList();

        return productRepository.saveAll(products);
    }
}
