package com.sellect.server.product.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.BrandRepository;
import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.CategoryRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.repository.OrderItemRepository;
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
import com.sellect.server.product.repository.InventoryRepository;
import com.sellect.server.product.repository.ProductImageRepository;
import com.sellect.server.product.repository.ProductRepository;
import com.sellect.server.product.util.StorageUtil;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final StorageClient storageClient;

    /**
     * 상품 단건 등록
     */
    @Transactional
    public ProductRegisterResponse register(
        User seller,
        ProductRegisterRequest request,
        List<MultipartFile> images) {

        // 존재하지 않는 카테고리 체크
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));

        // 존재하지 않는 브랜드 체크
        Brand brand = brandRepository.findById(request.brandId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "brand"));

        // 등록된 상품 기준 중복 검사 (sellerId, productName 기준)
        if (productRepository.isDuplicateProduct(seller.getId(), request.name())) {
            throw new CommonException(BError.EXIST, "product name");
        }

        Product product = productRepository.save(Product.register(
            seller,
            category,
            brand,
            request.getPriceAsBigDecimal(),
            request.name(),
            request.description()
        ));

        inventoryRepository.save(Inventory.register(product, request.stock()));

        // 이미지 저장 (이미지 이름에는 식별자(uuid)가 포함되어 있고, 이를 통해 image context 와 매핑)
        Map<String, MultipartFile> uuidToImageFile = new HashMap<>();
        images.forEach(image -> {
            String filename = image.getOriginalFilename();
            if (filename == null) {
                throw new CommonException(BError.NOT_EXIST, "file name");
            }
            uuidToImageFile.put(filename.substring(0, filename.lastIndexOf(".")), image);
        });
        request.imageContexts().forEach(imageContext -> {
            // 이미지 컨텍스트 정보에 있는 uuid 에 매핑되는 이미지 파일을 찾아서 저장
            MultipartFile imageFile = uuidToImageFile.get(imageContext.uuid());
            if (imageFile == null) {
                throw new CommonException(BError.NOT_MATCHES, "uuid in image context",
                    "uuid in image file name");
            }

            // 이미지 저장 (이미지 저장소 저장 -> 이미지 URL 가져오기 -> ProductImage 생성)
            String newFilename = StorageUtil.generateFileName(imageFile.getOriginalFilename());
            storageClient.store(imageFile, newFilename);
            ProductImage productImage = ProductImage.register(product,
                storageClient.loadAsPath(newFilename), imageContext);
            productImageRepository.save(productImage, product);
        });

        return ProductRegisterResponse.from(product);
    }

    /**
     * 상품 단건 등록 (이미지 별도)
     */
    @Transactional
    public ProductRegisterResponse register(
        User seller,
        ProductRegisterRequest request) {

        // 존재하지 않는 카테고리 체크
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));

        // 존재하지 않는 브랜드 체크
        Brand brand = brandRepository.findById(request.brandId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "brand"));

        // 등록된 상품 기준 중복 검사 (sellerId, productName 기준)
        if (productRepository.isDuplicateProduct(seller.getId(), request.name())) {
            throw new CommonException(BError.EXIST, "product name");
        }

        Product product = productRepository.save(Product.register(
            seller,
            category,
            brand,
            request.getPriceAsBigDecimal(),
            request.name(),
            request.description()
        ));

        inventoryRepository.save(Inventory.register(product, request.stock()));

        if (request.imageContexts().isEmpty()) {
            throw new CommonException(BError.REQUIRED, "image context");
        }
        request.imageContexts().forEach(imageContext -> {
            String imageUrl = storageClient.loadAsPath(imageContext.filename());
            ProductImage productImage = ProductImage.register(product, imageUrl, imageContext);
            productImageRepository.save(productImage, product);
        });

        return ProductRegisterResponse.from(product);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public ProductModifyResponse modify(Long sellerId, Long productId,
        ProductModifyRequest request) {

        // 수정할 상품이 존재하는지 확인
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));

        // 유저의 상품이 맞는지 확인
        product.validateSeller(sellerId);

        // 수정할 값이 존재할 경우만 수정
        Product modifiedProduct = product.modify(
            Optional.ofNullable(request.getPriceAsBigDecimal()).orElse(product.getPrice()),
            Optional.ofNullable(request.name()).orElse(product.getName()),
            Optional.ofNullable(request.description()).orElse(product.getDescription())
        );
        productRepository.save(modifiedProduct);

        Inventory inventory = getInventoryByProductId(productId);
        Inventory modifyInventory = inventory.modifyStock(
            Optional.ofNullable(request.stock()).orElse(inventory.getStock()));
        inventoryRepository.save(modifyInventory);

        return ProductModifyResponse.from(modifiedProduct, modifyInventory.getStock());
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void remove(Long sellerId, Long productId) {

        // 삭제할 상품이 존재하는지 확인
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));

        // 유저의 상품이 맞는지 확인
        product.validateSeller(sellerId);

        productRepository.save(product.remove());

        // 재고 삭제
        Inventory inventory = getInventoryByProductId(productId);
        inventoryRepository.save(inventory.deleteStock());
    }

    /**
     * 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductDetailRetrieveResponse retrieveDetail(Long productId) {
        // 상품 정보 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));

        Category smallCategory = categoryRepository.findById(product.getCategory().getId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "small category"));
        Category mediumCategory = categoryRepository.findById(smallCategory.getParentId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "medium category"));
        Category largeCategory = categoryRepository.findById(mediumCategory.getParentId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "large category"));

        // 브랜드명 조회
        Brand brand = brandRepository.findById(product.getBrand().getId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "brand"));

        // 이미지들 조회
        List<ProductImage> productImages = productImageRepository.findByProductId(productId);

        // 재고 조회
        Inventory inventory = getInventoryByProductId(productId);

        // todo: JPA가 알아서 조회
        return ProductDetailRetrieveResponse.from(
            product,
            smallCategory,
            mediumCategory,
            largeCategory,
            product.getSeller(),
            brand,
            inventory.getStock(),
            productImages
        );
    }

    //========================= Seller 전용 =========================//

    /**
     * 판매자의 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ProductDetailRetrieveResponse> retrieveAllBySeller(User seller, int page,
        int size) {

        Page<Product> products = productRepository.findBySellerId(seller.getId(),
            PageRequest.of(page, size));

        // TODO: 쿼리 최적화
        return products.map(product -> {
            Category smallCategory = categoryRepository.findById(product.getCategory().getId())
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));
            Category mediumCategory = categoryRepository.findById(smallCategory.getParentId())
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));
            Category largeCategory = categoryRepository.findById(mediumCategory.getParentId())
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));
            Brand brand = brandRepository.findById(product.getBrand().getId())
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "brand"));

            Inventory inventory = getInventoryByProductId(product.getId());

            List<ProductImage> productImages = productImageRepository.findByProductId(
                product.getId());
            return ProductDetailRetrieveResponse.from(
                product,
                smallCategory, mediumCategory, largeCategory,
                product.getSeller(), brand, inventory.getStock(),
                productImages);
        });
    }

    /**
     * 판매자의 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductDetailRetrieveBySellerResponse retrieveDetailBySeller(User seller,
        Long productId) {
        // 상품 정보 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "product"));

        // 유저의 상품이 맞는지 확인
        product.validateSeller(seller.getId());

        Category smallCategory = categoryRepository.findById(product.getCategory().getId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));
        Category mediumCategory = categoryRepository.findById(smallCategory.getParentId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));
        Category largeCategory = categoryRepository.findById(mediumCategory.getParentId())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category"));

        Inventory inventory = getInventoryByProductId(productId);
        List<ProductImage> productImages = productImageRepository.findByProductId(productId);
        Integer totalOrders = orderItemRepository.countCompleteOrdersByProductId(productId)
            .orElse(0);
        BigDecimal totalSales = orderItemRepository.calculateSalesByProductId(productId)
            .orElse(BigDecimal.ZERO);

        return ProductDetailRetrieveBySellerResponse.from(
            product, productImages, inventory.getStock(),
            smallCategory, mediumCategory, largeCategory,
            totalOrders, totalSales);
    }

    /**
     * 판매자의 상품 통계 조회
     */
    @Transactional(readOnly = true)
    public SellerStatsRetrieveResponse retrieveStats(User seller) {
        List<Long> productIds = productRepository.findProductIdsBySellerId(seller.getId());

        BigDecimal totalSales = orderItemRepository.calculateTotalSalesByProductIds(productIds)
            .orElse(BigDecimal.ZERO);

        return SellerStatsRetrieveResponse.from(totalSales, productIds.size());
    }

    @Transactional(readOnly = true)
    protected Inventory getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "inventory"));
    }

    /**
    * 상품 ID로 판매자 ID 조회
    */
    @Transactional(readOnly = true)
    public Set<Long> findSellerIdsByProductIds(List<Long> productIds) {
        return productRepository.findSellerIdByProductIds(productIds);
    }
}
