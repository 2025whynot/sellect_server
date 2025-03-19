package com.sellect.server.product.application;

import static com.sellect.server.product.application.FakeStorageClient.FAKE_IMAGE_STORAGE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.FakeBrandRepository;
import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.FakeCategoryRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.entity.OrderStatus;
import com.sellect.server.order.repository.fake.FakeOrderItemRepository;
import com.sellect.server.product.controller.request.ImageContextCreateRequest;
import com.sellect.server.product.controller.request.ProductModifyRequest;
import com.sellect.server.product.controller.request.ProductRegisterRequest;
import com.sellect.server.product.controller.response.ProductDetailRetrieveBySellerResponse;
import com.sellect.server.product.controller.response.ProductDetailRetrieveResponse;
import com.sellect.server.product.controller.response.ProductModifyResponse;
import com.sellect.server.product.controller.response.ProductRegisterResponse;
import com.sellect.server.product.controller.response.SellerStatsRetrieveResponse;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import com.sellect.server.product.repository.FakeInventoryRepository;
import com.sellect.server.product.repository.FakeProductImageRepository;
import com.sellect.server.product.repository.FakeProductRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

class ProductServiceTest {

    private final FakeProductRepository productRepository = new FakeProductRepository();
    private final FakeBrandRepository brandRepository = new FakeBrandRepository();
    private final FakeCategoryRepository categoryRepository = new FakeCategoryRepository();
    private final FakeProductImageRepository productImageRepository = new FakeProductImageRepository();
    private final FakeOrderItemRepository orderItemRepository = new FakeOrderItemRepository();
    private final FakeStorageClient storageClient = new FakeStorageClient();
    private final FakeInventoryRepository inventoryRepository = new FakeInventoryRepository();
    private final ProductService sut = new ProductService(
        productRepository, brandRepository, categoryRepository,
        productImageRepository, orderItemRepository, inventoryRepository, storageClient
    );

    private User seller;
    private Category largeCategory;
    private Category mediumCategory;
    private Category smallCategory;
    private Brand brand;

    @BeforeEach
    void setUp() {
        productRepository.clear();
        brandRepository.clear();
        categoryRepository.clear();
        productImageRepository.clear();
        orderItemRepository.clear();
        inventoryRepository.clear();

        seller = User.builder()
            .id(1L)
//            .uuid(UUID.randomUUID().toString())
            .nickname("seller")
            .role(Role.SELLER)
            .build();

        // 대분류 (Electronics)
        largeCategory = Category.builder()
            .name("Electronics")
            .parentId(null) // 최상위 카테고리
            .build();
        largeCategory = categoryRepository.save(largeCategory);

        // 중분류 (Home Appliances)
        mediumCategory = Category.builder()
            .name("Home Appliances")
            .parentId(largeCategory.getId()) // 대분류를 부모로 참조
            .build();
        mediumCategory = categoryRepository.save(mediumCategory);

        // 소분류 (TV)
        smallCategory = Category.builder()
            .name("TV")
            .parentId(mediumCategory.getId()) // 중분류를 부모로 참조
            .build();
        smallCategory = categoryRepository.save(smallCategory);

        brand = Brand.builder()
            .name("Samsung")
            .build();
        brand = brandRepository.save(brand);
    }

    @AfterEach
    void tearDown() {
        productRepository.clear();
        brandRepository.clear();
        categoryRepository.clear();
        productImageRepository.clear();
        storageClient.deleteAll();
    }

    @Nested
    @DisplayName("상품 등록 테스트 (이미지 포함)")
    class RegisterWithImagesTests {

        @Test
        @DisplayName("이미지와 함께 상품을 성공적으로 등록")
        void register_WithImages_Success() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(3L)
                .brandId(1L)
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .uuid("image1-uuid")
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("image1-uuid.jpg")
                        .build(),
                    ImageContextCreateRequest.builder()
                        .uuid("image2-uuid")
                        .sequence(2)
                        .isRepresentative(false)
                        .filename("image2-uuid.jpg")
                        .build()
                ))
                .build();
            MultipartFile image1 = new FakeMultipartFile("image1-uuid.jpg");
            MultipartFile image2 = new FakeMultipartFile("image2-uuid.jpg");

            // When
            ProductRegisterResponse result = sut.register(seller, request, List.of(image1, image2));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("TV");
            System.out.println(productRepository.findById(result.productId()));
            assertThat(productRepository.findById(result.productId())).hasValueSatisfying(
                product -> {
                    assertThat(product.getSeller().getId()).isEqualTo(seller.getId());
                    assertThat(product.getPrice()).isEqualTo(new BigDecimal("100.00"));
                });
            assertThat(
                productImageRepository.findByProductId(result.productId()).stream()
                    .sorted(Comparator.comparingInt(ProductImage::getSequence)))
                .hasSize(2)
                .satisfies(images -> {
                    assertThat(images.get(0).getImageUrl()).contains("image1-uuid");
                    assertThat(images.get(0).getImageUrl()).matches(
                        FAKE_IMAGE_STORAGE_URL + "image1-uuid_\\d+\\.jpg");
                    assertThat(images.get(0).getSequence()).isEqualTo(1);
                    assertThat(images.get(0).isRepresentative()).isTrue();
                    assertThat(images.get(1).getImageUrl()).contains("image2-uuid");
                    assertThat(images.get(1).getImageUrl()).matches(
                        FAKE_IMAGE_STORAGE_URL + "image2-uuid_\\d+\\.jpg");
                    assertThat(images.get(1).getSequence()).isEqualTo(2);
                    assertThat(images.get(1).isRepresentative()).isFalse();
                });
            assertThat(inventoryRepository.findByProductId(result.productId()))
                .hasValueSatisfying(inventory -> {
                    assertThat(inventory.getStock()).isEqualTo(10);
                });
        }

        @Test
        @DisplayName("이미지의 파일 이름이 null 이면 예외 발생")
        void register_NullFileName_ThrowsException() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(3L)
                .brandId(1L)
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .uuid("image1-uuid")
                        .sequence(1)
                        .isRepresentative(true)
                        .build()
                ))
                .build();
            // 파일 이름이 null 인 가짜 이미지 생성
            MultipartFile nullFileNameImage = new FakeMultipartFile(null) {
                @Override
                public String getOriginalFilename() {
                    return null; // 파일 이름 null 로 강제 설정
                }
            };

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request, List.of(nullFileNameImage)))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("file name"));
        }

        @Test
        @DisplayName("이미지 UUID가 매핑되지 않으면 예외 발생")
        void register_UnmatchedImageUuid_ThrowsException() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(brand.getId())
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .uuid("image1-uuid")
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("image1-uuid.jpg")
                        .build()
                ))
                .build();
            MultipartFile image = new FakeMultipartFile("wrong-uuid.jpg"); // 매핑되지 않음

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request, List.of(image)))
                .isInstanceOf(CommonException.class)
                .hasMessage(
                    BError.NOT_MATCHES.getMessage("uuid in image context",
                        "uuid in image file name"));
        }

        @Test
        @DisplayName("이미지가 없는데 UUID가 포함된 경우 예외 발생")
        void register_WithUuidButNoImage_ThrowsException() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(brand.getId())
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .uuid("image1-uuid")
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("image1-uuid.jpg")
                        .build()
                ))
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request, Collections.emptyList()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_MATCHES.getMessage("uuid in image context",
                    "uuid in image file name"));
        }

        @Test
        @DisplayName("중복된 상품명으로 등록 시 예외 발생")
        void register_DuplicateProductName_ThrowsException() {
            // Given
            Product existingProduct = Product.register(
                seller, smallCategory, brand, new BigDecimal("100.00"), "TV", "Old TV"
            );
            productRepository.save(existingProduct);

            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(brand.getId())
                .price("200.00")
                .name("TV")
                .description("New TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("default-image.jpg")
                        .build()
                ))
                .build();
            MultipartFile multipartFile = new FakeMultipartFile("image-uuid.jpg");

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request, List.of(multipartFile)))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.EXIST.getMessage("product name"));
        }
    }

    @Nested
    @DisplayName("상품 등록 테스트 (이미지는 별도 저장)")
    class RegisterWithoutImagesTests {

        @Test
        @DisplayName("이미지를 별도 저장 후 상품을 성공적으로 등록")
        void register_AfterSavingImages_Success() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(brand.getId())
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("image-uuid.jpg")
                        .build()
                ))
                .build();
            MultipartFile imageFile = new FakeMultipartFile("image-uuid.jpg");
            storageClient.store(imageFile, "image-uuid.jpg"); // 이미지를 별도로 저장할 때는 timestamp 가 붙지 않음

            // When
            ProductRegisterResponse result = sut.register(seller, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("TV");
            assertThat(productImageRepository.findByProductId(result.productId()))
                .hasSize(1)
                .first()
                .satisfies(image -> {
                    assertThat(image.getImageUrl()).contains("image-uuid");
                    assertThat(image.getImageUrl()).isEqualTo(
                        FAKE_IMAGE_STORAGE_URL + "image-uuid.jpg");
                    assertThat(image.getSequence()).isEqualTo(1);
                    assertThat(image.isRepresentative()).isTrue();
                });
            assertThat(inventoryRepository.findByProductId(result.productId()))
                .hasValueSatisfying(inventory -> {
                    assertThat(inventory.getStock()).isEqualTo(10);
                });
        }

        @Test
        @DisplayName("ImageContext가 없는 경우 예외 발생")
        void register_NoImageContext_ThrowsException() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(brand.getId())
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(Collections.emptyList())
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.REQUIRED.getMessage("image context"));
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 등록 시 예외 발생")
        void register_NonExistentCategory_ThrowsException() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(999L)
                .brandId(brand.getId())
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("default-image.jpg")
                        .build()
                ))
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("category"));
        }

        @Test
        @DisplayName("존재하지 않는 브랜드로 등록 시 예외 발생")
        void register_NonExistentBrand_ThrowsException() {
            // Given
            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(999L)
                .price("100.00")
                .name("TV")
                .description("Smart TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("default-image.jpg")
                        .build()
                ))
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("brand"));
        }

        @Test
        @DisplayName("중복된 상품명으로 등록 시 예외 발생")
        void register_DuplicateProductName_ThrowsException() {
            // Given
            Product existingProduct = Product.register(
                seller, smallCategory, brand, new BigDecimal("100.00"), "TV", "Old TV"
            );
            productRepository.save(existingProduct);

            ProductRegisterRequest request = ProductRegisterRequest.builder()
                .categoryId(smallCategory.getId())
                .brandId(brand.getId())
                .price("200.00")
                .name("TV")
                .description("New TV")
                .stock(10)
                .imageContexts(List.of(
                    ImageContextCreateRequest.builder()
                        .sequence(1)
                        .isRepresentative(true)
                        .filename("default-image.jpg")
                        .build()
                ))
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.register(seller, request))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.EXIST.getMessage("product name"));
        }
    }

    @Nested
    @DisplayName("상품 수정 테스트")
    class ModifyTests {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.register(seller, smallCategory, brand, new BigDecimal("100.00"), "TV",
                "Smart TV");
            product = productRepository.save(product);
            inventoryRepository.save(Inventory.builder()
                .product(product)
                .stock(10)
                .build());
        }

        @Test
        @DisplayName("상품을 성공적으로 수정")
        void modify_Success() {
            // Given
            ProductModifyRequest request = ProductModifyRequest.builder()
                .price("150.00")
                .name("Updated TV")
                .description("Updated Smart TV")
                .stock(20)
                .build();

            // When
            ProductModifyResponse result = sut.modify(seller.getId(), product.getId(), request);

            // Then
            assertThat(result.name()).isEqualTo("Updated TV");
            assertThat(productRepository.findById(result.productId())).hasValueSatisfying(p -> {
                assertThat(p.getPrice()).isEqualTo(new BigDecimal("150.00"));
                assertThat(p.getDescription()).isEqualTo("Updated Smart TV");
            });
            assertThat(inventoryRepository.findByProductId(result.productId()))
                .hasValueSatisfying(inventory -> {
                    assertThat(inventory.getStock()).isEqualTo(20);
                });
        }

        @Test
        @DisplayName("일부 필드만 수정")
        void modify_PartialUpdate_Success() {
            // Given
            ProductModifyRequest request = ProductModifyRequest.builder()
                .price("150.00")
                .build();

            // When
            ProductModifyResponse result = sut.modify(seller.getId(), product.getId(), request);

            // Then
            assertThat(result.name()).isEqualTo("TV");
            assertThat(productRepository.findById(result.productId())).hasValueSatisfying(p -> {
                assertThat(p.getPrice()).isEqualTo(new BigDecimal("150.00"));
                assertThat(p.getDescription()).isEqualTo("Smart TV");
            });
            assertThat(inventoryRepository.findByProductId(result.productId()))
                .hasValueSatisfying(inventory -> {
                    assertThat(inventory.getStock()).isEqualTo(10);
                });
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정 시 예외 발생")
        void modify_NonExistentProduct_ThrowsException() {
            // Given
            ProductModifyRequest request = ProductModifyRequest.builder()
                .price("150.00")
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.modify(seller.getId(), 999L, request))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("product"));
        }

        @Test
        @DisplayName("권한 없는 사용자가 수정 시 예외 발생")
        void modify_UnauthorizedSeller_ThrowsException() {
            // Given
            User otherSeller = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.SELLER)
                .build();
            ProductModifyRequest request = ProductModifyRequest.builder().price("150.00").build();

            // When & Then
            assertThatThrownBy(() -> sut.modify(otherSeller.getId(), product.getId(), request))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.ACCESS_DENIED.getMessage("product"));
        }
    }

    @Nested
    @DisplayName("상품 삭제 테스트")
    class RemoveTests {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.register(seller, smallCategory, brand, new BigDecimal("100.00"), "TV",
                "Smart TV");
            product = productRepository.save(product);
            inventoryRepository.save(Inventory.builder()
                .product(product)
                .stock(10)
                .build());
        }

        @Test
        @DisplayName("상품을 성공적으로 삭제")
        void remove_Success() {
            // When
            sut.remove(seller.getId(), product.getId());

            // Then
            assertThat(productRepository.findById(product.getId())).isEmpty();
            assertThat(inventoryRepository.findByProductId(product.getId())).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제 시 예외 발생")
        void remove_NonExistentProduct_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> sut.remove(seller.getId(), 999L))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("product"));
        }

        @Test
        @DisplayName("권한 없는 사용자가 삭제 시 예외 발생")
        void remove_UnauthorizedSeller_ThrowsException() {
            // Given
            User otherSeller = User.builder().id(2L)
                .role(Role.SELLER).build();

            // When & Then
            assertThatThrownBy(() -> sut.remove(otherSeller.getId(), product.getId()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.ACCESS_DENIED.getMessage("product"));
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 테스트")
    class RetrieveDetailTests {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.register(seller, smallCategory, brand, new BigDecimal("100.00"), "TV",
                "Smart TV");
            product = productRepository.save(product);
            inventoryRepository.save(Inventory.builder()
                .product(product)
                .stock(10)
                .build());
            ProductImage image = ProductImage.register(
                product,
                "/path/to/image.jpg",
                ImageContextCreateRequest.builder()
                    .sequence(1)
                    .isRepresentative(true)
                    .filename("image.jpg")
                    .build()
            );
            productImageRepository.save(image, product);
        }

        @Test
        @DisplayName("상품 상세를 성공적으로 조회")
        void retrieveDetail_Success() {
            // When
            ProductDetailRetrieveResponse result = sut.retrieveDetail(product.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("TV");
            assertThat(result.images()).hasSize(1);
            assertThat(result.stock()).isEqualTo(10);
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
        void retrieveDetail_NonExistentProduct_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetail(999L))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("product"));
        }

        @Test
        @DisplayName("상품의 소분류가 존재하지 않을 때 예외 발생 (DB 문제)")
        void retrieveDetail_NonExistentSmallCategory_ThrowsException() {
            // Given
            Category nonExistentSmallCategory = Category.builder().id(999L).build();
            product = productRepository.save(Product.builder()
                .id(product.getId())
                .category(nonExistentSmallCategory)
                .build());

            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetail(product.getId()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("small category"));
        }

        @Test
        @DisplayName("상품의 중분류가 존재하지 않을 때 예외 발생 (DB 문제)")
        void retrieveDetail_NonExistentMediumCategory_ThrowsException() {
            // Given
            Category nonExistentMediumCategory = Category.builder().id(999L).build();
            smallCategory = categoryRepository.save(Category.builder()
                .id(smallCategory.getId())
                .parentId(nonExistentMediumCategory.getId())
                .build());
            product = productRepository.save(Product.builder()
                .id(product.getId())
                .category(smallCategory)
                .build());

            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetail(product.getId()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("medium category"));
        }

        @Test
        @DisplayName("상품의 대분류가 존재하지 않을 때 예외 발생 (DB 문제)")
        void retrieveDetail_NonExistentLargeCategory_ThrowsException() {
            // Given
            Category nonExistentLargeCategory = Category.builder().id(999L).build();
            mediumCategory = categoryRepository.save(Category.builder()
                .id(mediumCategory.getId())
                .parentId(nonExistentLargeCategory.getId())
                .build());
            smallCategory = categoryRepository.save(Category.builder()
                .id(smallCategory.getId())
                .parentId(mediumCategory.getId())
                .build());
            product = productRepository.save(Product.builder()
                .id(product.getId())
                .category(smallCategory)
                .build());

            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetail(product.getId()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("large category"));
        }

    }

    @Nested
    @DisplayName("판매자의 상품 목록 조회 테스트")
    class RetrieveAllBySellerTests {

        Product product1;
        Product product2;

        @BeforeEach
        void setUp() {
            // 테스트 데이터 추가
            product1 = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("100.00"), "TV1", "TV 1 Description"
            );
            product2 = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("200.00"), "TV2", "TV 2 Description"
            );
            product1 = productRepository.save(product1);
            product2 = productRepository.save(product2);

            // 이미지 추가
            ProductImage image1 = ProductImage.register(product1,
                FAKE_IMAGE_STORAGE_URL + "image1.jpg",
                ImageContextCreateRequest.builder().sequence(1).isRepresentative(true)
                    .filename("image1.jpg").build());
            ProductImage image2 = ProductImage.register(product2,
                FAKE_IMAGE_STORAGE_URL + "image2.jpg",
                ImageContextCreateRequest.builder().sequence(1).isRepresentative(true)
                    .filename("image2.jpg").build());
            productImageRepository.save(image1, product1);
            productImageRepository.save(image2, product2);

            // 재고 추가
            inventoryRepository.save(Inventory.builder()
                .product(product1)
                .stock(10)
                .build());
            inventoryRepository.save(Inventory.builder()
                .product(product2)
                .stock(5)
                .build());
        }

        @Test
        @DisplayName("판매자의 상품 목록을 성공적으로 조회 - 모든 필드 검증")
        void retrieveAllBySeller_Success() {
            // When
            Page<ProductDetailRetrieveResponse> result = sut.retrieveAllBySeller(seller, 0, 10);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.getContent()).satisfiesExactlyInAnyOrder(
                response -> {
                    assertThat(response.productId()).isEqualTo(product1.getId());
                    assertThat(response.smallCategoryName()).isEqualTo("TV");
                    assertThat(response.mediumCategoryName()).isEqualTo("Home Appliances");
                    assertThat(response.largeCategoryName()).isEqualTo("Electronics");
                    assertThat(response.brandName()).isEqualTo("Samsung");
                    assertThat(response.sellerName()).isEqualTo("seller");
                    assertThat(response.name()).isEqualTo("TV1");
                    assertThat(response.price()).isEqualTo(new BigDecimal("100.00"));
                    assertThat(response.description()).isEqualTo("TV 1 Description");
                    assertThat(response.stock()).isEqualTo(10);
                    assertThat(response.images()).hasSize(1)
                        .first().satisfies(image -> {
                            assertThat(image.imageUrl()).isEqualTo(
                                FAKE_IMAGE_STORAGE_URL + "image1.jpg");
                            assertThat(image.sequence()).isEqualTo(1);
                            assertThat(image.representative()).isTrue();
                        });
                },
                response -> {
                    assertThat(response.productId()).isEqualTo(product2.getId());
                    assertThat(response.smallCategoryName()).isEqualTo("TV");
                    assertThat(response.mediumCategoryName()).isEqualTo("Home Appliances");
                    assertThat(response.largeCategoryName()).isEqualTo("Electronics");
                    assertThat(response.brandName()).isEqualTo("Samsung");
                    assertThat(response.sellerName()).isEqualTo("seller");
                    assertThat(response.name()).isEqualTo("TV2");
                    assertThat(response.price()).isEqualTo(new BigDecimal("200.00"));
                    assertThat(response.description()).isEqualTo("TV 2 Description");
                    assertThat(response.stock()).isEqualTo(5);
                    assertThat(response.images()).hasSize(1)
                        .first().satisfies(image -> {
                            assertThat(image.imageUrl()).isEqualTo(
                                FAKE_IMAGE_STORAGE_URL + "image2.jpg");
                            assertThat(image.sequence()).isEqualTo(1);
                            assertThat(image.representative()).isTrue();
                        });
                }
            );
        }

        @Test
        @DisplayName("빈 상품 목록 조회")
        void retrieveAllBySeller_EmptyList() {
            // Given
            User otherSeller = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.SELLER)
                .build();

            // When
            Page<ProductDetailRetrieveResponse> result = sut.retrieveAllBySeller(otherSeller, 0,
                10);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제된 상품은 제외")
        void retrieveAllBySeller_ExcludesDeleted() {
            // Given
            Product deletedProduct = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("300.00"), "TV3", "TV 3 Description"
            );
            productRepository.save(deletedProduct.remove());

            // When
            Page<ProductDetailRetrieveResponse> result = sut.retrieveAllBySeller(seller, 0, 10);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.getContent())
                .extracting(ProductDetailRetrieveResponse::name)
                .containsExactlyInAnyOrder("TV1", "TV2")
                .doesNotContain("TV3");
        }

        @Test
        @DisplayName("페이지네이션 동작 확인")
        void retrieveAllBySeller_Pagination() {
            // When
            Page<ProductDetailRetrieveResponse> result = sut.retrieveAllBySeller(seller, 0, 1);

            // Then
            assertThat(result).hasSize(1); // 첫 페이지에 1개만 반환
            assertThat(result.getTotalElements()).isEqualTo(2); // 전체 2개
            assertThat(result.getTotalPages()).isEqualTo(2); // 총 2페이지
            assertThat(result.getContent().get(0).name()).isIn("TV1", "TV2"); // TV1 또는 TV2 중 하나
        }
    }

    @Nested
    @DisplayName("판매자의 상품 상세 조회 테스트")
    class RetrieveDetailBySellerTests {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("100.00"), "TV", "Smart TV"
            );
            product = productRepository.save(product);
            ProductImage image = ProductImage.register(product,
                FAKE_IMAGE_STORAGE_URL + "image1.jpg",
                ImageContextCreateRequest.builder().sequence(1).isRepresentative(true)
                    .filename("image1.jpg").build());
            productImageRepository.save(image, product);

            inventoryRepository.save(Inventory.builder()
                .product(product)
                .stock(10)
                .build());
        }

        @Test
        @DisplayName("판매자의 상품 상세를 성공적으로 조회")
        void retrieveDetailBySeller_Success() {
            // When
            ProductDetailRetrieveBySellerResponse result = sut.retrieveDetailBySeller(seller,
                product.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("TV");
            assertThat(result.price()).isEqualTo(new BigDecimal("100.00"));
            assertThat(result.description()).isEqualTo("Smart TV");
            assertThat(result.stock()).isEqualTo(10);
            assertThat(result.smallCategoryName()).isEqualTo("TV");
            assertThat(result.mediumCategoryName()).isEqualTo("Home Appliances");
            assertThat(result.largeCategoryName()).isEqualTo("Electronics");
            assertThat(result.images()).hasSize(1)
                .first().satisfies(image -> {
                    assertThat(image.imageUrl()).isEqualTo(FAKE_IMAGE_STORAGE_URL + "image1.jpg");
                    assertThat(image.sequence()).isEqualTo(1);
                    assertThat(image.representative()).isTrue();
                });
            assertThat(result.totalOrders()).isEqualTo(0);
            assertThat(result.totalSales()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
        void retrieveDetailBySeller_NonExistentProduct_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetailBySeller(seller, 999L))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("product"));
        }

        @Test
        @DisplayName("권한 없는 사용자가 조회 시 예외 발생")
        void retrieveDetailBySeller_UnauthorizedSeller_ThrowsException() {
            // Given
            User otherSeller = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.SELLER)
                .build();

            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetailBySeller(otherSeller, product.getId()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.ACCESS_DENIED.getMessage("product"));
        }

        @Test
        @DisplayName("삭제된 상품 조회 시 예외 발생")
        void retrieveDetailBySeller_DeletedProduct_ThrowsException() {
            // Given
            productRepository.save(product.remove());

            // When & Then
            assertThatThrownBy(() -> sut.retrieveDetailBySeller(seller, product.getId()))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("product"));
        }
    }

    @Nested
    @DisplayName("판매자의 상품 통계 조회 테스트")
    class RetrieveStatsTests {

        Product product1;
        Product product2;

        @BeforeEach
        void setUp() {
            product1 = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("100.00"), "TV1", "TV 1"
            );
            product2 = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("200.00"), "TV2", "TV 2"
            );
            product1 = productRepository.save(product1);
            product2 = productRepository.save(product2);
        }

        @Test
        @DisplayName("판매자의 상품 통계를 성공적으로 조회")
        void retrieveStats_Success() {
            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSales()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.totalProductsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("상품이 없는 경우 통계 조회")
        void retrieveStats_NoProducts() {
            // Given
            User otherSeller = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.SELLER)
                .build();

            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(otherSeller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSales()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.totalProductsCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("삭제된 상품은 통계에서 제외")
        void retrieveStats_ExcludesDeleted() {
            // Given
            Product deletedProduct = Product.register(
                seller, smallCategory, brand,
                new BigDecimal("300.00"), "TV3", "TV 3"
            );
            productRepository.save(deletedProduct.remove());

            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalProductsCount()).isEqualTo(2); // 삭제된 상품 제외
            assertThat(result.totalSales()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("주문 데이터가 있는 경우 통계 조회 - 단일 주문")
        void retrieveStats_WithSingleOrder() {
            // Given: 주문 데이터 추가
            User user = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.USER)
                .build();
            Orders completedOrder = Orders.register(user, new BigDecimal("400.00"),
                OrderStatus.COMPLETED);
            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(completedOrder)
                .productId(product1.getId())
                .price(new BigDecimal("100.00"))
                .quantity(2)
                .build());
            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(completedOrder)
                .productId(product2.getId())
                .price(new BigDecimal("200.00"))
                .quantity(1)
                .build());

            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSales()).isEqualTo(
                new BigDecimal("400.00")); // (100 * 2) + (200 * 1)
            assertThat(result.totalProductsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("주문 데이터가 있는 경우 통계 조회 - 다중 주문")
        void retrieveStats_WithMultipleOrders() {
            // Given: 여러 주문 데이터 추가
            User user1 = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.USER)
                .build();
            User user2 = User.builder()
                .id(3L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.USER)
                .build();
            Orders completedOrder = Orders.register(user1, new BigDecimal("400.00"),
                OrderStatus.COMPLETED);
            Orders pendingOrder = Orders.register(user2, new BigDecimal("50.00"),
                OrderStatus.PENDING);

            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(completedOrder)
                .productId(product1.getId())
                .price(new BigDecimal("100.00"))
                .quantity(2) // 200
                .build());
            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(completedOrder)
                .productId(product2.getId())
                .price(new BigDecimal("200.00"))
                .quantity(1) // 200
                .build());
            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(pendingOrder)
                .productId(product2.getId())
                .price(new BigDecimal("50.00"))
                .quantity(1) // PENDING 이므로 제외
                .build());

            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSales()).isEqualTo(new BigDecimal("400.00"));
            assertThat(result.totalProductsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("주문 데이터가 있는 경우 통계 조회 - 일부 상품만 주문")
        void retrieveStats_WithPartialOrders() {
            // Given: product1에만 주문 데이터 추가
            User user = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.USER)
                .build();
            Orders completedOrder = Orders.register(user, new BigDecimal("300.00"),
                OrderStatus.COMPLETED);
            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(completedOrder)
                .productId(product1.getId())
                .price(new BigDecimal("100.00"))
                .quantity(3) // 300
                .build());

            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSales()).isEqualTo(new BigDecimal("300.00")); // product1만 포함
            assertThat(result.totalProductsCount()).isEqualTo(2); // 전체 상품 수는 변함없음
        }

        @Test
        @DisplayName("주문 데이터가 있는 경우 통계 조회 - PENDING 주문 제외")
        void retrieveStats_ExcludesPendingOrders() {
            // Given: PENDING 상태 주문만 추가
            User user = User.builder()
                .id(2L)
//                .uuid(UUID.randomUUID().toString())
                .role(Role.USER)
                .build();
            Orders pendingOrder = Orders.register(user, new BigDecimal("500.00"),
                OrderStatus.PENDING);
            orderItemRepository.addOrderItem(OrderItem.builder()
                .orders(pendingOrder)
                .productId(product1.getId())
                .price(new BigDecimal("100.00"))
                .quantity(5) // 500, 하지만 PENDING 이므로 제외
                .build());

            // When
            SellerStatsRetrieveResponse result = sut.retrieveStats(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSales()).isEqualTo(BigDecimal.ZERO); // PENDING 주문 제외
            assertThat(result.totalProductsCount()).isEqualTo(2);
        }
    }
}