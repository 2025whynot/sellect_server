package com.sellect.server.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.controller.request.ImageContextUpdateRequest;
import com.sellect.server.product.controller.request.ProductImageModifyRequest;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import com.sellect.server.product.repository.FakeProductImageRepository;
import com.sellect.server.product.repository.FakeProductRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class ProductImageServiceTest {

    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final FakeProductImageRepository productImageRepository = new FakeProductImageRepository();
    private final ProductImageService sut = new ProductImageService(
        new FakeStorageService(),
        productRepository,
        productImageRepository);

    private final Long sellerId = 1L;
    private final Long productId = 100L;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
            .id(productId)
            .name("Test Product")
            .seller(User.builder()
                .id(sellerId)
                .build())
            .build();

        // productImage1 -> productImage2 -> productImage3 (순서 보장)
        ProductImage productImage1 = ProductImage.builder()
            .id(1L)
            .uuid("uuid1")
            .next("uuid2")
            .imageUrl("image-url1")
            .product(product)
            .build();
        ProductImage productImage2 = ProductImage.builder()
            .id(1L)
            .uuid("uuid2")
            .prev("uuid1")
            .next("uuid3")
            .imageUrl("image-url2")
            .product(product)
            .build();
        ProductImage productImage3 = ProductImage.builder()
            .id(1L)
            .uuid("uuid3")
            .prev("uuid2")
            .next("uuid4")
            .imageUrl("image-url3")
            .product(product)
            .build();

        productRepository.save(product);
        productImageRepository.save(productImage1, product);
        productImageRepository.save(productImage2, product);
        productImageRepository.save(productImage3, product);
    }

    @Nested
    @DisplayName("상품 이미지 수정 테스트")
    class ModifyTests {

        @Test
        @DisplayName("첫 번째 상품 이미지 삭제")
        void modifyProductImages_Success() {
            // Given
            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(productId)
                .toDelete(List.of("uuid1"))
                .toUpdate(List.of(ImageContextUpdateRequest.builder()
                    .target("uuid2")
                    .prev(null)
                    .next("uuid3")
                    .build()))
                .build();

            // When
            sut.modifyProductImages(sellerId, request, Collections.emptyList());

            // Then
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid1")).isEmpty();
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid2"))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isNull();
                    assertThat(image.getNext()).isEqualTo("uuid3");
                });
        }

        @Test
        @DisplayName("두 번째 상품 이미지 삭제 후 두 번째 위치에 새로운 상품 이미지 삽입")
        void modifyProductImages_DeleteSecondImageAndInsertNewImage() {
            // Given
            String newImageUuid = "new-image-uuid";
            MultipartFile newImage = mock(MultipartFile.class);
            given(newImage.getName()).willReturn(newImageUuid);
            given(newImage.getOriginalFilename()).willReturn("new-image.jpg");

            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(productId)
                .toDelete(List.of("uuid2"))
                .toUpdate(List.of(
                    ImageContextUpdateRequest.builder()
                        .target("uuid1")
                        .next(newImageUuid)
                        .build(),
                    ImageContextUpdateRequest.builder()
                        .target(newImageUuid)
                        .prev("uuid1")
                        .next("uuid3")
                        .isNewImage(true)
                        .build(),
                    ImageContextUpdateRequest.builder()
                        .target("uuid3")
                        .prev(newImageUuid)
                        .build()

                ))
                .build();

            // When
            sut.modifyProductImages(sellerId, request, List.of(newImage));

            // Then
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid2")).isEmpty();
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid1"))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isNull();
                    assertThat(image.getNext()).isEqualTo(newImageUuid);
                });
            assertThat(productImageRepository.findByProductIdAndUuid(productId, newImageUuid))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isEqualTo("uuid1");
                    assertThat(image.getNext()).isEqualTo("uuid3");
                });
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid3"))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isEqualTo(newImageUuid);
                    assertThat(image.getNext()).isNull();
                });
        }

        @Test
        @DisplayName("첫 번째와 두 번째 이미지 사이에 새로운 이미지 삽입")
        void modifyProductImages_InsertNewImageBetweenFirstAndSecondImage() {
            // Given
            String newImageUuid = "new-image-uuid";
            MultipartFile newImage = mock(MultipartFile.class);
            given(newImage.getName()).willReturn(newImageUuid);
            given(newImage.getOriginalFilename()).willReturn("new-image.jpg");

            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(productId)
                .toDelete(Collections.emptyList())
                .toUpdate(List.of(
                    ImageContextUpdateRequest.builder()
                        .target("uuid1")
                        .next(newImageUuid)
                        .build(),
                    ImageContextUpdateRequest.builder()
                        .target(newImageUuid)
                        .prev("uuid1")
                        .next("uuid2")
                        .isNewImage(true)
                        .build(),
                    ImageContextUpdateRequest.builder()
                        .target("uuid2")
                        .prev(newImageUuid)
                        .next("uuid3")
                        .build()
                ))
                .build();

            // When
            sut.modifyProductImages(sellerId, request, List.of(newImage));

            // Then
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid1"))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isNull();
                    assertThat(image.getNext()).isEqualTo(newImageUuid);
                });
            assertThat(productImageRepository.findByProductIdAndUuid(productId, newImageUuid))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isEqualTo("uuid1");
                    assertThat(image.getNext()).isEqualTo("uuid2");
                });
            assertThat(productImageRepository.findByProductIdAndUuid(productId, "uuid2"))
                .hasValueSatisfying(image -> {
                    assertThat(image.getPrev()).isEqualTo(newImageUuid);
                    assertThat(image.getNext()).isEqualTo("uuid3");
                });
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외 발생")
        void modifyProductImages_ProductNotFound() {
            // Given
            Long nonExistentProductId = 999L;
            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(nonExistentProductId)
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.modifyProductImages(sellerId, request, Collections.emptyList()))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(BError.NOT_EXIST.getMessage("product"));
        }

        @Test
        @DisplayName("판매자 권한이 없으면 예외 발생")
        void modifyProductImages_Forbidden() {
            // Given
            Long anotherSellerId = 999L;
            Product anotherSellerProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .seller(User.builder()
                    .id(anotherSellerId)
                    .build())
                .build();
            productRepository.save(anotherSellerProduct);

            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(productId)
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.modifyProductImages(sellerId, request, Collections.emptyList()))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("seller doesn't have permission");
        }

        @Test
        @DisplayName("삭제할 이미지가 존재하지 않으면 예외 발생")
        void modifyProductImages_ImageToDeleteNotFound() {
            // Given
            String nonExistentUuid = "non-existent-uuid";
            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(productId)
                .toDelete(List.of(nonExistentUuid))
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.modifyProductImages(sellerId, request, Collections.emptyList()))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(BError.NOT_EXIST.getMessage("product image"));
        }

        @Test
        @DisplayName("추가할 이미지의 UUID가 존재하지 않으면 예외 발생")
        void modifyProductImages_ImageToAddNotFound() {
            // Given
            String nonExistentUuid = "non-existent-uuid";
            MultipartFile mockImage = mock(MultipartFile.class);
            given(mockImage.getName()).willReturn(nonExistentUuid);

            ProductImageModifyRequest request = ProductImageModifyRequest.builder()
                .productId(productId)
                .toDelete(Collections.emptyList())
                .toUpdate(List.of(ImageContextUpdateRequest.builder()
                    .target(nonExistentUuid)
                    .build()))
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.modifyProductImages(sellerId, request, List.of(mockImage)))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(BError.NOT_EXIST.getMessage("product image"));
        }
    }
}
