package com.sellect.server.product.controller;

import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.product.application.ProductService;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductRegisterResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /*
    * 상품 등록 (복수 지원)
    * */
    @PostMapping("/product")
    // todo : sellerId token 에서 가져오도록 변경할 것!!
    // todo : 상품 이미지 관련 로직 추가할 것!!
    public ApiResponse<ProductRegisterResponse> registerMultiple(Long sellerId,
        @RequestBody List<ProductRegisterRequest> requests) {

        ProductRegisterResponse result = productService.registerMultiple(sellerId,
            requests);
        return ApiResponse.OK(result);
    }
}
