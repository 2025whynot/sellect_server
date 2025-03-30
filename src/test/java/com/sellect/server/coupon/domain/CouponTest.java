package com.sellect.server.coupon.domain;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.repository.entity.CouponStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CouponTest {

    @Nested
    @DisplayName("[성공] isUsable() ")
    class CouponIsUsable{

        @Test
        @DisplayName("[성공] 쿠폰이 사용가능한지 판단")
        void willSuccess() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("test")
                .role(Role.SELLER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(1L)
                .seller(user)
                .discountCost(3000)
                .quantity(10)
                .expirationDate(LocalDate.now().plusDays(10))
                .couponStatus(CouponStatus.IN_STOCK)
                .build();


            // when & then
            assertThatNoException().isThrownBy(coupon::validateDownload);
        }

        @Test
        @DisplayName("[실패] 쿠폰 수량이 0 이하인 경우 사용 불가능")
        void quantityLowerThanOne() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("test")
//                .uuid("uuid")
                .role(Role.SELLER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(1L)
                .seller(user)
                .discountCost(3000)
                .quantity(0)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();

            // when
            CommonException exception = assertThrows(CommonException.class, coupon::validateDownload);

            // then
            assertThat(exception.getErrorType()).isEqualTo(BError.class);
            assertThat(exception.getMessage()).isEqualTo("The quantity of the coupon1 is 0");
        }


        @Test
        @DisplayName("[실패] 쿠폰 유효날짜가 오늘보다 낮을 경우 불가능")
        void willFailWhenExpiredDate() {
            //given
            User user = User.builder()
                .id(1L)
                .nickname("test")
//                .uuid("uuid")
                .role(Role.SELLER)
                .build();

            Coupon coupon = Coupon.builder()
                .id(30L)
                .seller(user)
                .discountCost(3000)
                .quantity(100)
                .expirationDate(LocalDate.now().minusDays(30))
                .couponStatus(CouponStatus.IN_STOCK)
                .build();

            // when
            CommonException exception = assertThrows(CommonException.class, coupon::validateDownload);

            // then
            assertThat(exception.getErrorType()).isEqualTo(BError.class);
            assertThat(exception.getMessage()).isEqualTo("The coupon30 has expired");
        }

    }
}