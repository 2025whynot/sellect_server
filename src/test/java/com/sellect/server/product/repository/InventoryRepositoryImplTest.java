//package com.sellect.server.product.repository;
//
//import static org.assertj.core.api.BDDAssertions.then;
//
//import com.sellect.server.auth.repository.entity.Role;
//import com.sellect.server.auth.repository.entity.UserEntity;
//import com.sellect.server.auth.repository.user.UserJpaRepository;
//import com.sellect.server.brand.repository.BrandEntity;
//import com.sellect.server.brand.repository.BrandJpaRepository;
//import com.sellect.server.category.repository.CategoryEntity;
//import com.sellect.server.category.repository.CategoryJpaRepository;
//import com.sellect.server.config.JpaConfig;
//import com.sellect.server.config.JsonConfig;
//import com.sellect.server.product.domain.Inventory;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.transaction.annotation.Transactional;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@DataJpaTest
//@Testcontainers
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@Import({JpaConfig.class, JsonConfig.class})
//class InventoryRepositoryImplTest {
//
//    @Container
//    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
//        .withDatabaseName("test-db")
//        .withUsername("test")
//        .withPassword("test");
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        mysql.start();
//        System.out.println("MySQL Running: " + mysql.isRunning());
//        System.out.println("JDBC URL: " + mysql.getJdbcUrl());
//        registry.add("spring.datasource.url", mysql::getJdbcUrl);
//        registry.add("spring.datasource.username", mysql::getUsername);
//        registry.add("spring.datasource.password", mysql::getPassword);
//        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
//        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQL8Dialect");
//        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true"); // SQL 포맷팅
//        registry.add("logging.level.org.springframework.transaction", () -> "TRACE"); // 트랜잭션 로깅
//    }
//
//    @Autowired
//    private InventoryJpaRepository inventoryJpaRepository;
//    private InventoryRepositoryImpl inventoryRepository;
//
//    @Autowired
//    private UserJpaRepository userJpaRepository;
//
//    @Autowired
//    private CategoryJpaRepository categoryJpaRepository;
//
//    @Autowired
//    private BrandJpaRepository brandJpaRepository;
//
//    @Autowired
//    private ProductJpaRepository productJpaRepository;
//
//    private UserEntity sellerEntity;
//    private CategoryEntity categoryEntity;
//    private BrandEntity brandEntity;
//    private ProductEntity productEntity;
//    private InventoryEntity inventoryEntity;
//
//    @BeforeEach
//    void setUp() {
//        inventoryRepository = new InventoryRepositoryImpl(inventoryJpaRepository);
//
//        sellerEntity = UserEntity.builder()
//            .uuid("test-seller-uuid-" + System.currentTimeMillis())
//            .nickname("Test Seller")
//            .role(Role.SELLER)
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build();
//        userJpaRepository.save(sellerEntity);
//
//        categoryEntity = CategoryEntity.builder()
//            .name("Test Category")
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build();
//        categoryJpaRepository.save(categoryEntity);
//
//        brandEntity = BrandEntity.builder()
//            .name("Test Brand")
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build();
//        brandJpaRepository.save(brandEntity);
//
//        productEntity = ProductEntity.builder()
//            .name("Test Product")
//            .sellerEntity(sellerEntity)
//            .categoryEntity(categoryEntity)
//            .brandEntity(brandEntity)
//            .price(new BigDecimal("50000"))
//            .description("Test Description")
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build();
//        productJpaRepository.save(productEntity);
//
//        inventoryEntity = InventoryEntity.builder()
//            .productEntity(productEntity)
//            .stock(100)
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build();
//        inventoryJpaRepository.save(inventoryEntity);
//    }
//
//    @Nested
//    @DisplayName("findWithLockByProductId()")
//    class FindWithLockByProductIdTest {
//
//        @Test
//        @DisplayName("비관적 읽기 락(PESSIMISTIC_READ)이 읽기는 허용하고 쓰기는 차단하는지 확인")
//        @Transactional
//        void testPessimisticReadLock() throws InterruptedException {
//            // given
//            Long productId = productEntity.getId();
//            ExecutorService executorService = Executors.newFixedThreadPool(2);
//            CountDownLatch latch = new CountDownLatch(2);
//            AtomicBoolean readSuccess = new AtomicBoolean(false); // 읽기 성공 여부 기록
//
//            // 트랜잭션 1: 락을 걸고 재고를 읽음
//            Runnable transaction1 = () -> {
//                Thread currentThread = Thread.currentThread();
//                System.out.println(currentThread.getName() + ": Transaction 1 started, State = "
//                    + currentThread.getState());
//                inventoryRepository.findWithLockByProductId(productId).ifPresent(inventory -> {
//                    System.out.println(
//                        "Transaction 1: Locked inventory with quantity " + inventory.getStock());
//                    System.out.println(currentThread.getName() + ": State after lock = "
//                        + currentThread.getState());
//                    try {
//                        Thread.sleep(2000); // 2초 동안 락 유지
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        System.out.println(currentThread.getName() + ": Interrupted, State = "
//                            + currentThread.getState());
//                    }
//                });
//                latch.countDown();
//                System.out.println(currentThread.getName() + ": Transaction 1 completed, State = "
//                    + currentThread.getState());
//            };
//
//            // 트랜잭션 2: 락이 걸린 상태에서 읽기 및 쓰기 시도
//            Runnable transaction2 = () -> {
//                Thread currentThread = Thread.currentThread();
//                try {
//                    Thread.sleep(500); // 트랜잭션 1이 락을 잡을 때까지 대기
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    System.out.println(currentThread.getName() + ": Interrupted, State = "
//                        + currentThread.getState());
//                }
//
//                System.out.println(currentThread.getName() + ": Transaction 2 started, State = "
//                    + currentThread.getState());
//                // 읽기 시도 (허용됨)
//                Optional<Inventory> readInventory = inventoryRepository.findWithLockByProductId(
//                    productId);
//                if (readInventory.isPresent()) {
//                    readSuccess.set(true); // 읽기 성공 시 플래그 설정
//                    System.out.println("Transaction 2: Successfully read inventory with quantity "
//                        + readInventory.get().getStock());
//                } else {
//                    System.out.println("Transaction 2: Failed to read inventory!");
//                }
//                System.out.println(
//                    currentThread.getName() + ": State after read = " + currentThread.getState());
//
//                // 쓰기 시도 (차단되어야 함)
//                try {
//                    inventoryRepository.findWithLockByProductId(productId).ifPresent(inventory -> {
//                        Inventory updatedInventory = Inventory.builder()
//                            .id(inventory.getId())
//                            .product(inventory.getProduct())
//                            .stock(50) // 수정 시도
//                            .createdAt(inventory.getCreatedAt())
//                            .updatedAt(LocalDateTime.now())
//                            .build();
//                        inventoryRepository.save(updatedInventory);
//                        System.out.println("Transaction 2: Unexpectedly modified inventory!");
//                    });
//                } catch (Exception e) {
//                    System.out.println(
//                        "Transaction 2: Write blocked as expected: " + e.getMessage());
//                }
//                latch.countDown();
//                System.out.println(currentThread.getName() + ": Transaction 2 completed, State = "
//                    + currentThread.getState());
//            };
//
//            // when
//            executorService.submit(transaction1);
//            executorService.submit(transaction2);
//
//            // then
//            boolean completed = latch.await(10, TimeUnit.SECONDS); // 최대 10초 대기
//            executorService.shutdownNow(); // 강제 종료
//            executorService.awaitTermination(1, TimeUnit.SECONDS); // 종료 대기
//
//            if (!completed) {
//                System.out.println("Test timed out waiting for transactions to complete");
//                throw new RuntimeException("Test timed out");
//            }
//
//            Inventory finalInventory = inventoryRepository.findWithLockByProductId(productId).get();
//            then(readSuccess.get()).isTrue(); // 읽기가 실패하면 안 됨을 검증
//            then(finalInventory.getStock()).isEqualTo(100); // 수정이 차단되었으므로 원래 값 유지
//        }
//    }
//}