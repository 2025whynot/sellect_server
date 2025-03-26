package com.sellect.server.coupon.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.infrastructure.annotation.AuthSeller;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.coupon.application.CouponService;
import com.sellect.server.coupon.controller.request.IssueCouponRequest;
import com.sellect.server.coupon.controller.response.ActiveCouponResponse;
import com.sellect.server.coupon.controller.response.CouponPossibleOrderResponse;
import com.sellect.server.coupon.controller.response.CouponResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public ApiResponse<?> issueCoupon(
        @AuthSeller User user,
        @RequestBody IssueCouponRequest issueCouponRequest
    ) {
        couponService.uploadCoupon(user, issueCouponRequest);
        return ApiResponse.ok(null);
    }

    @PutMapping("/register/{couponId}")
    public ApiResponse<?> registerCoupon(@AuthUser User user, @PathVariable Long couponId) {
        couponService.downloadCoupon(user, couponId);
        return ApiResponse.ok();
    }


    // 사용자가 등록한 쿠폰 내역 조회
    @GetMapping
    public ApiResponse<List<CouponResponse>> getCoupon(@AuthUser User user,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) Boolean isUsed) {
        List<CouponResponse> couponList = couponService.listUserReceivedCoupons(user, page, size, isUsed);
        return ApiResponse.ok(couponList);
    }


    // 사용자가 등록 가능한 쿠폰 리스트 조회
    @GetMapping("/actives")
    public ApiResponse<Page<ActiveCouponResponse>> getActiveCouponList(
        @AuthUser User user,
        @PageableDefault(page = 0, size = 5) Pageable pageable
    ) {
        Page<ActiveCouponResponse> activeCouponList = couponService.listDownloadableCouponsForUser(user,
            pageable);
        return ApiResponse.ok(activeCouponList);
    }

    @GetMapping("/possible-order")
    public ApiResponse<List<CouponPossibleOrderResponse>> getPossibleOrderCouponList(
        @AuthUser User user, @RequestParam("productIds") List<Long> productIds
    ) {
        List<CouponPossibleOrderResponse> couponList = couponService.getUsableCouponsForProducts(
            user, productIds);
        return ApiResponse.ok(couponList);
    }


    // 애플리케이션 락
    @PutMapping("/register/{couponId}/app/{userId}")
    public ApiResponse<?> registerCoupon(@PathVariable(name = "userId") Long userId,
        @PathVariable(name = "couponId") Long couponId) {
        User user = User.builder()
            .id(userId)
//            .uuid(String.valueOf(UUID.randomUUID()))
//            .uuid(String.valueOf(UUID.randomUUID()))
            .nickname("test" + userId)
            .role(Role.USER)
            .build();
        couponService.downloadCouponv2(user, couponId);
        return ApiResponse.ok();
    }


    // DB 비관적 락
    // test용
    @PutMapping("/register/{couponId}/db/{userId}")
    public ApiResponse<?> downloadCouponWithDBLock(@PathVariable(name = "userId") Long userId,
        @PathVariable(name = "couponId") Long couponId) {
        User user = User.builder()
            .id(userId)
//            .uuid(String.valueOf(UUID.randomUUID()))
            .nickname("test" + userId)
            .role(Role.USER)
            .build();
        couponService.downloadCouponWithPessimisticLock(user, couponId);
        return ApiResponse.ok();
    }

    // Redis 분산락
    // test 용
    @PutMapping("/register/{couponId}/redis/{userId}")
    public ApiResponse<?> downloadCouponWithRedis(@PathVariable(name = "userId") Long userId,
        @PathVariable(name = "couponId") Long couponId) {
        User user = User.builder()
            .id(userId)
//            .uuid(String.valueOf(UUID.randomUUID()))
            .nickname("test" + userId)
            .role(Role.USER)
            .build();
//        couponService.downloadCouponWithDistributeLock(user, couponId);
//        couponService.downloadCouponWithRedis(user, couponId);
        couponService.downloadCouponWithRedisV2(user, couponId);
        return ApiResponse.ok();
    }

}
