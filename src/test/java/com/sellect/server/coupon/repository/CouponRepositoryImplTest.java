package com.sellect.server.coupon.repository;


import static org.assertj.core.api.BDDAssertions.then;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.repository.entity.CouponStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional
class CouponRepositoryImplTest {

    @Autowired
    EntityManager em;

    @Autowired
    private CouponJpaRepository couponJpaRepository;
    private CouponRepositoryImpl couponRepositoryImpl;
    UserEntity sellerEntity;

    @BeforeEach
    void setUp() {
        couponRepositoryImpl = new CouponRepositoryImpl(couponJpaRepository);
        sellerEntity = UserEntity.from(User.register("seller", Role.SELLER));
        em.persist(sellerEntity);
        em.flush();
    }


    @Test
    @DisplayName("쿠폰 저장 테스트")
    void couponSaveSuccess() {
        //given
        Coupon coupon = Coupon.builder()
            .id(1L)
            .seller(sellerEntity.toModel())
            .discountCost(1000)
            .quantity(10)
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(LocalDateTime.now().minusDays(1))
            .expirationDate(LocalDate.now().plusDays(7))
            .build();

        //when
        couponRepositoryImpl.save(coupon);

        //then
        Coupon savedCoupon = couponRepositoryImpl.findById(1L).orElse(null);
        assert savedCoupon != null;
        then(savedCoupon.getId()).isEqualTo(1L);
        then(savedCoupon.getSeller().getNickname()).isEqualTo("seller");
        then(savedCoupon.getDiscountCost()).isEqualTo(1000);
        then(savedCoupon.getQuantity()).isEqualTo(10);
    }

    @Nested
    @DisplayName("쿠폰 조회 테스트")
    class CouponFind {

        @Test
        @DisplayName("쿠폰 조회 성공")
        void couponFindSuccess() {
            //given
            Coupon coupon = Coupon.builder()
                .seller(sellerEntity.toModel())
                .discountCost(1000)
                .quantity(10)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .expirationDate(LocalDate.now().plusDays(7))
                .build();
            Coupon savedCoupon = couponRepositoryImpl.save(coupon);

            //when
            Coupon findCoupon = couponRepositoryImpl.findById(savedCoupon.getId()).orElse(null);

            //then
            then(findCoupon.getId()).isEqualTo(savedCoupon.getId());
            then(findCoupon.getSeller().getNickname()).isEqualTo("seller");
            then(findCoupon.getDiscountCost()).isEqualTo(1000);
            then(findCoupon.getQuantity()).isEqualTo(10);
        }
    }


    @Nested
    @DisplayName("쿠폰 목록 조회 테스트")
    class CouponFindAll {

        @Test
        @DisplayName("[성공] 다운로드 가능한 쿠폰 조회")
        void couponFindAllSuccess() {
            //given
            Coupon coupon1 = Coupon.builder()
                .seller(sellerEntity.toModel())
                .discountCost(1000)
                .quantity(10)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .expirationDate(LocalDate.now().plusDays(7))
                .couponStatus(CouponStatus.IN_STOCK)
                .build();

            Coupon coupon2 = Coupon.builder()
                .seller(sellerEntity.toModel())
                .discountCost(2000)
                .quantity(20)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .expirationDate(LocalDate.now().plusDays(3))
                .couponStatus(CouponStatus.IN_STOCK)
                .build();


            Coupon coupon3 = Coupon.builder()
                .seller(sellerEntity.toModel())
                .discountCost(2000)
                .quantity(20)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .expirationDate(LocalDate.now().plusDays(3))
                .couponStatus(CouponStatus.OUT_OF_STOCK)
                .build();

            Coupon savedCoupon1 = couponRepositoryImpl.save(coupon1);
            Coupon savedCoupon2 = couponRepositoryImpl.save(coupon2);
            Coupon savedCoupon3 = couponRepositoryImpl.save(coupon3);

            //when
            PageRequest request = PageRequest.of(0, 10);
            Page<Coupon> allActiveCouponList = couponRepositoryImpl.findAllActiveCouponList(
                request);



            //then
            then(allActiveCouponList.getContent().size()).isEqualTo(2);
            then(allActiveCouponList.getContent().get(0).getId()).isEqualTo(savedCoupon1.getId());
            then(allActiveCouponList.getContent().get(1).getId()).isEqualTo(savedCoupon2.getId());
        }

    }


    @Nested
    @DisplayName("쿠폰 수량 감소 테스트")
    class CouponDecreaseQuantity {

        @Test
        @DisplayName("쿠폰 수량 감소 성공")
        void couponDecreaseQuantitySuccess() {
            // given
            Coupon coupon = Coupon.builder()
                .seller(sellerEntity.toModel())
                .discountCost(1000)
                .quantity(10)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .expirationDate(LocalDate.now().plusDays(7))
                .build();
            Coupon savedCoupon = couponRepositoryImpl.save(coupon); // 저장 후 반환된 객체 사용

            // when
            Coupon decreasedCoupon = savedCoupon.decreaseQuantity();
            couponRepositoryImpl.save(decreasedCoupon);

            // then
            Coupon foundCoupon = couponRepositoryImpl.findById(savedCoupon.getId()).orElse(null);
            then(foundCoupon).isNotNull();
            then(foundCoupon.getQuantity()).isEqualTo(9);
        }

    }

}

