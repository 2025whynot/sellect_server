package com.sellect.server.product.application;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.controller.request.ImageContextUpdateRequest;
import com.sellect.server.product.controller.request.ProductImageModifyRequest;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import com.sellect.server.product.repository.ProductImageRepository;
import com.sellect.server.product.repository.ProductRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final StorageService storageService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public void modifyProductImages(
        Long sellerId,
        ProductImageModifyRequest request,
        List<MultipartFile> images) {

        Long productId = request.productId();
        List<String> toDelete = request.toDelete();
        List<ImageContextUpdateRequest> toUpdate = request.toUpdate();

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));

        if (!Objects.equals(product.getSeller().getId(), sellerId)) {
            throw new CommonException(BError.FAIL_FOR_REASON,
                "modify product images",
                "seller doesn't have permission to modify product images");
        }

        // 상품 이미지 삭제
        toDelete.forEach(uuid -> {
            ProductImage productImage = productImageRepository.findByProductIdAndUuid(productId, uuid)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product image"));
            productImageRepository.save(productImage.remove(), product);
        });


        // 상품 이미지 추가
        Map<String, String> newFileNames = new HashMap<>();
        images.forEach(image -> {
            // TODO: 클라이언트 측에서 input 태그의 name 을 uuid 로 설정해야만 정상 동작하는데,
            //  추후 클라이언트에 의존적이지 않은 방법으로 수정해야 함
            String newFileName = generateFileName(image.getName(), image.getOriginalFilename());
            newFileNames.put(image.getName(), newFileName);
            storageService.store(image, newFileName);
        });

        // 상품 이미지 수정 (이미지 순서 변경)
        toUpdate.forEach(updateRequest -> {
            if (updateRequest.isNewImage()) {
                String imageUrl = storageService.loadAsPath(newFileNames.get(updateRequest.target()));
                ProductImage productImage = ProductImage.registerWhenUpdate(product, imageUrl, updateRequest);
                productImageRepository.save(productImage, product);
            } else {
                ProductImage productImage = productImageRepository.findByProductIdAndUuid(productId, updateRequest.target())
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product image"));
                ProductImage updatedProductImage = productImage.update(updateRequest);
                productImageRepository.save(updatedProductImage, product);
            }
        });
    }

    private String generateFileName(String fileName, String originalFileName) {
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return fileName + "_" + System.currentTimeMillis() + fileExtension;
    }
}
