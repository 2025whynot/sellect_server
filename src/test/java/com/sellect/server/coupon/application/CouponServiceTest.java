package com.sellect.server.coupon.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.coupon.controller.request.IssueCouponRequest;
import com.sellect.server.coupon.controller.response.ActiveCouponResponse;
import com.sellect.server.coupon.controller.response.CouponPossibleOrderResponse;
import com.sellect.server.coupon.controller.response.CouponResponse;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.CouponRepository;
import com.sellect.server.coupon.repository.FakeCouponRepository;
import com.sellect.server.coupon.repository.FakeuserReceivedCouponRepository;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.FakeProductRepository;
import com.sellect.server.product.repository.ProductRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

class CouponServiceTest {

    CouponService couponService;
    CouponRepository couponRepository;
    UserReceivedCouponRepository userReceivedCouponRepository;
    ProductRepository productRepository;
    RedissonClient redissonClient;
    PlatformTransactionManager platformTransactionManager;

    @BeforeEach
    void setUp() {
        platformTransactionManager = mock(PlatformTransactionManager.class);
//        redissonClient = mock(RedissonClient.class);
        couponRepository = new FakeCouponRepository();
        userReceivedCouponRepository = new FakeuserReceivedCouponRepository();
        productRepository = new FakeProductRepository();
        couponService = new CouponService(platformTransactionManager, couponRepository,
            userReceivedCouponRepository, productRepository, redissonClient);
//        couponService = new CouponService(platformTransactionManager, couponRepository, userReceivedCouponRepository, productRepository);
    }


    @Nested
    @DisplayName("issueCoupon() 메소드는")
    class issueCouponTest {

        @Test
        @DisplayName("유저가 판매자가 아닐때 exception을 던진다.")
        void userIsNotSellerThrowsException() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("test")
                .uuid("uuid")
                .role(Role.USER)
                .build();

            //when
            Exception exception = assertThrows(CommonException.class, () -> {
                couponService.uploadCoupon(user,
                    new IssueCouponRequest(1, 10, LocalDate.now().plusDays(10)));
            });

            //then
            assertEquals("test is not a seller", exception.getMessage());
        }

        @Test
        @DisplayName("쿠폰을 정상적으로 등록한다")
        void _willSuccess() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("test")
                .uuid("uuid")
                .role(Role.SELLER)
                .build();

            IssueCouponRequest request = new IssueCouponRequest(10, 3000,
                LocalDate.now().plusDays(10));

            //when
            couponService.uploadCoupon(user, request);

            //then
            assertEquals(10, couponRepository.findById(1L).get().getQuantity());
            assertEquals(3000, couponRepository.findById(1L).get().getDiscountCost());
        }
    }


    @Nested
    @DisplayName("downloadCoupon() 메소드는")
    class RegisterCouponTest {

        @Test
        @DisplayName("쿠폰을 정상적으로 유저가 등록한다")
        void _willSuccess() {
            //given
            long couponId = 1L;
            User user = User.builder()
                .id(1L)
                .nickname("test")
                .uuid("uuid")
                .role(Role.SELLER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(couponId)
                .seller(user)
                .discountCost(3000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();
            couponRepository.save(coupon);

            //when
            couponService.downloadCoupon(user, couponId);

            //then
            assertEquals(9, couponRepository.findById(couponId).get().getQuantity());
        }


        @Test
        @DisplayName("쿠폰 수량이 부족하면 exception을 던진다")
        void couponQuantityLowerThenZeroThrowException() {
            //given
            long couponId = 1L;
            User user = User.builder()
                .id(1L)
                .nickname("test")
                .uuid("uuid")
                .role(Role.USER)
                .build();

            User anotherUser = User.builder()
                .id(2L)
                .nickname("test2")
                .uuid("uuid333")
                .role(Role.USER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(couponId)
                .seller(user)
                .discountCost(3000)
                .quantity(1)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();
            couponRepository.save(coupon);

            //when
            couponService.downloadCoupon(user, couponId);

            //when & then
            CommonException commonException = assertThrows(CommonException.class,
                () -> couponService.downloadCoupon(anotherUser, couponId));

            assertEquals(String.format("The quantity of the coupon%s is 0", couponId),
                commonException.getMessage());
        }

        @Test
        @DisplayName("한 사용자가 쿠폰을 중복으로 등록하면 exception을 던진다")
        void whenUserTriesToRegisterCouponTwice_thenThrowsException() {
            //given
            long couponId = 1L;
            User seller = User.builder()
                .id(5L)
                .nickname("test")
                .uuid("uuid")
                .role(Role.SELLER)
                .build();

            User user = User.builder()
                .id(1L)
                .nickname("test")
                .uuid("uuid")
                .role(Role.USER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(couponId)
                .seller(seller)
                .discountCost(3000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();

            couponRepository.save(coupon);
            //when
            couponService.downloadCoupon(user, couponId);

            CommonException exception = assertThrows(CommonException.class, () -> {
                couponService.downloadCoupon(user, couponId);
            });

            //then
            assertEquals(String.format("The coupon%s has already been registered", couponId),
                exception.getMessage());
        }


        @ParameterizedTest
        @CsvSource({"10, 10", "300, 100"})
        @DisplayName("사용자가 한번에 여러명이 들어올 경우 쿠폰 등록자 숫자만큼 쿠폰개수를 삭감한다.")
        void concurrentCouponRegistrationReducesQuantityCorrectly(int couponQuantity,
            int threadCount) throws InterruptedException {
            //given
            long couponId = 1;
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(threadCount);
            User seller = User.builder().id(1L).nickname("test").role(Role.SELLER).build();

            Coupon coupon = Coupon.builder()
                .id(couponId)
                .seller(seller)
                .discountCost(3000)
                .quantity(couponQuantity)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();
            couponRepository.save(coupon);

            //when
            for (int i = 0; i < threadCount; i++) {
                final long userId = i;
                executorService.submit(() -> {
                    try {
                        User register = User.builder()
                            .id(userId)
                            .nickname("user id_" + userId)
                            .role(Role.USER).build();
                        couponService.downloadCoupon(register, 1L);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executorService.shutdown();

            //then
            assertEquals(couponQuantity - threadCount,
                couponRepository.findById(1L).get().getQuantity());
        }

    }

    @Nested
    @DisplayName("쿠폰 내역 가져오기 테스트")
    class GetReceivedCouponListTest {

        @Test
        @DisplayName("사용자가 사용하지 않고 등록한 쿠폰을 조회한다.")
        void _willSuccess() {
            // Given
            User user = User.builder()
                .id(1L)
                .nickname("testUser")
                .role(Role.USER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(1L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(3000)
                .quantity(1)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();

            Coupon anotherCoupon = Coupon.builder()
                .id(2L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(5000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();

            Coupon useCoupon = Coupon.builder()
                .id(10L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(3200)
                .quantity(20)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();

            couponRepository.save(coupon);
            couponRepository.save(anotherCoupon);
            couponRepository.save(useCoupon);

            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            UserReceivedCoupon userReceivedCoupon2 = UserReceivedCoupon.create(user, anotherCoupon);
            UserReceivedCoupon userReceivedCoupon3 = UserReceivedCoupon.create(user, useCoupon);
            UserReceivedCoupon usedCoupon = userReceivedCoupon3.useCoupon();

            userReceivedCouponRepository.save(userReceivedCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon2);
            userReceivedCouponRepository.save(usedCoupon);

            // when
            List<CouponResponse> couponList = couponService.getCouponList(user, 0, 5, false);

            // then
            assertEquals(2, couponList.size());
        }

        @Test
        @DisplayName("만료된 쿠폰은 조회되지 않는다")
        void expiredCouponNotIncluded() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("testUser")
                .role(Role.USER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(1L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(3000)
                .quantity(1)
                .expirationDate(LocalDate.now().minusDays(3))
                .build();

            Coupon anotherCoupon = Coupon.builder()
                .id(2L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(5000)
                .quantity(10)
                .expirationDate(LocalDate.now().minusDays(20))
                .build();

            //when
            couponRepository.save(coupon);
            couponRepository.save(anotherCoupon);
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            UserReceivedCoupon userReceivedCoupon2 = UserReceivedCoupon.create(user, anotherCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon2);

            List<CouponResponse> couponList = couponService.getCouponList(user, 0, 5, false);

            //then
            then(couponList).isEmpty();
        }

        @Test
        @DisplayName("사용된 쿠폰이 포함되지 않는다")
        void usedCouponNotIncluded() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("testUser")
                .role(Role.USER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(1L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(3000)
                .quantity(1)
                .expirationDate(LocalDate.now().plusDays(3))
                .build();

            Coupon anotherCoupon = Coupon.builder()
                .id(2L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(5000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(20))
                .build();

            couponRepository.save(coupon);
            couponRepository.save(anotherCoupon);
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            UserReceivedCoupon userReceivedCoupon2 = UserReceivedCoupon.create(user, anotherCoupon);
            userReceivedCoupon2 = userReceivedCoupon2.useCoupon();
            userReceivedCouponRepository.save(userReceivedCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon2);

            //when
            List<CouponResponse> couponList = couponService.getCouponList(user, 0, 5, false);

            //then
            then(couponList.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("쿠폰이 없는 경우 빈 리스트 반환")
        void noCouponsReturnsEmptyList() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("testUser")
                .role(Role.USER)
                .build();

            //when
            List<CouponResponse> couponList = couponService.getCouponList(user, 0, 5, false);

            //then
            then(couponList).isEmpty();
        }
    }

    @Nested
    @DisplayName("getActiveCouponList() 메소드는")
    class GetActiveCouponLiset {

        @Test
        @DisplayName("유저가 등록 할 수 있는 쿠폰 목록을 조회한다.")
        void willSuccess() {
            //given
            Coupon coupon = Coupon.builder()
                .id(1L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(3000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .build();

            Coupon coupon2 = Coupon.builder()
                .id(3L)
                .seller(User.builder().id(2L).nickname("seller").role(Role.SELLER).build())
                .discountCost(10000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();

            couponRepository.save(coupon);
            couponRepository.save(coupon2);
            Pageable pageable = PageRequest.of(0, 5);

            //when
            Page<ActiveCouponResponse> activeCouponList = couponService.getActiveCouponList(
                User.builder().id(1L).role(Role.USER).build(), pageable);

            //then
            assertEquals(2, activeCouponList.getContent().size());
//            assertEquals(10000, activeCouponList.getContent().get(0).couponInfo().discountCost());
//            assertEquals(3000, activeCouponList.getContent().get(1).couponInfo().discountCost());
        }
    }

    @Nested
    @DisplayName("getCouponsByMatchingSeller() 메소드는")
    class GetCouponsByMatchingSeller {

        @Test
        @DisplayName("판매자가 등록한 상품에 매칭되는 쿠폰 목록을 조회한다.")
        void willSuccess() {
            //given
            User user = User.builder().id(1L).nickname("test").role(Role.USER).build();
            User seller1 = User.builder().id(2L).nickname("seller").role(Role.SELLER).build();
            User seller2 = User.builder().id(5L).nickname("anotherSseller").role(Role.SELLER)
                .build();

            Coupon coupon1 = Coupon.builder()
                .id(1L)
                .seller(seller1)
                .discountCost(3000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();

            Coupon coupon2 = Coupon.builder()
                .id(3L)
                .seller(seller1)
                .discountCost(10000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .build();

            Coupon coupon3 = Coupon.builder()
                .id(3L)
                .seller(seller2)
                .discountCost(10000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();

            Product product1 = Product.builder()
                .id(1L)
                .seller(seller1)
                .build();

            Product product2 = Product.builder()
                .id(2L)
                .seller(seller1)
                .build();

            UserReceivedCoupon userReceivedCoupon1 = UserReceivedCoupon.builder()
                .id(1L)
                .user(user)
                .coupon(coupon1)
                .isUsed(false)
                .build();
            UserReceivedCoupon userReceivedCoupon2 = UserReceivedCoupon.builder()
                .id(2L)
                .user(user)
                .coupon(coupon2)
                .isUsed(false)
                .build();
            UserReceivedCoupon userReceivedCoupon3 = UserReceivedCoupon.builder()
                .id(3L)
                .user(user)
                .coupon(coupon3)
                .isUsed(false)
                .build();

            productRepository.save(product1);
            productRepository.save(product2);
            couponRepository.save(coupon1);
            couponRepository.save(coupon2);
            userReceivedCouponRepository.save(userReceivedCoupon1);
            userReceivedCouponRepository.save(userReceivedCoupon2);
            userReceivedCouponRepository.save(userReceivedCoupon3);

            List<Long> productIds = List.of(1L, 2L);

            //when
            List<CouponPossibleOrderResponse> couponList = couponService.getCouponsByMatchingSeller(
                user, productIds);

            //then
            couponList.forEach(System.out::println);
            then(couponList.size()).isEqualTo(2);
            then(couponList.get(0).userReceivedCouponId()).isEqualTo(2L);
            then(couponList.get(1).userReceivedCouponId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 상품 ID가 포함된 경우 예외를 던진다.")
        void willFailWithNonExistentProductId() {
            // given
            User user = User.builder().id(1L).nickname("test").role(Role.USER).build();
            User seller1 = User.builder().id(2L).nickname("seller").role(Role.SELLER).build();

            Product product1 = Product.builder()
                .id(1L)
                .seller(seller1)
                .build();

            // 존재하는 상품만 저장
            productRepository.save(product1);

            List<Long> productIds = List.of(1L, 999L); // 999L은 존재하지 않음

            // when & then
            assertThatThrownBy(() -> couponService.getCouponsByMatchingSeller(user, productIds))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("999");
        }
    }


}