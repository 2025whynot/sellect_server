package com.sellect.server.product.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthSeller;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.product.application.ProductImageService;
import com.sellect.server.product.application.ProductService;
import com.sellect.server.product.controller.request.ProductImageModifyRequest;
import com.sellect.server.product.controller.request.ProductModifyRequest;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductModifyResponse;
import com.sellect.server.product.controller.response.ProductRegisterResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;

    /**
     * 상품 등록 (복수 지원)
     * */
    @PostMapping("/product")
    // todo : sellerId token 에서 가져오도록 변경할 것!!
    // todo : 상품 이미지 관련 로직 추가할 것!!
    // todo : seller 완료 후엔 수정
    public ApiResponse<ProductRegisterResponse> registerMultiple(@AuthSeller User seller,
        @Valid @RequestBody List<ProductRegisterRequest> requests) {

        ProductRegisterResponse result = productService.registerMultiple(seller,
            requests);
        return ApiResponse.ok(result);
    }

    /**
     * 상품 단건 수정 API
     */
    @PatchMapping("/products/{productId}")
    public ApiResponse<ProductModifyResponse> modify(
        @AuthSeller User seller,
        @PathVariable Long productId,
        @Valid @RequestBody ProductModifyRequest request
    ) {
        ProductModifyResponse response = productService.modify(seller.getId(), productId, request);
        return ApiResponse.ok(response);
    }

    /**
     * 상품 단건 삭제 API
     */
    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> remove(
        @AuthSeller User seller,
        @PathVariable Long productId
    ) {
        productService.remove(seller.getId(), productId);
        return ApiResponse.ok();
    }

    /**
     * 상품 이미지 수정 API
     * */
    @PostMapping("/products/images")
    public ApiResponse<Void> modifyProductImages(
        @AuthSeller User seller,
        @RequestParam("images") List<MultipartFile> images,
        @RequestBody ProductImageModifyRequest request
    ) {
        productImageService.modifyProductImages(seller.getId(), request, images);
        return ApiResponse.ok();
    }

}
