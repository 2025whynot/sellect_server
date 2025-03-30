package com.sellect.server.coupon.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.application.downloadcoupon.DownLoadCoupon;
import com.sellect.server.coupon.controller.request.IssueCouponRequest;
import com.sellect.server.coupon.controller.response.ActiveCouponResponse;
import com.sellect.server.coupon.controller.response.CouponInfo;
import com.sellect.server.coupon.controller.response.CouponPossibleOrderResponse;
import com.sellect.server.coupon.controller.response.CouponResponse;
import com.sellect.server.coupon.controller.response.SellerInfo;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.CouponRepository;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private static final Sort DEFAULT_SORT = Sort.by(Direction.DESC, "createdAt");
    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;
    private final DownLoadCoupon downLoadCoupon;

    // 상속 보다는 컴포지션
    public void downloadCoupon(User user, Long couponId) {
        downLoadCoupon.downloadWithRedisSet(user, couponId);
    }

    public void downloadCouponReentrantLock(User user, Long couponId) {
        downLoadCoupon.couponDownloadWithReentrantLock(user, couponId);
    }

    public void downloadCouponPessimisticLock(User user, Long couponId) {
        downLoadCoupon.setCouponDownloadWithPessimisticLock(user, couponId);
    }

    public void downloadCouponDistributeLock(User user, Long couponId) {
        downLoadCoupon.setCouponDownloadWithDistributedLock(user, couponId);
    }

    public void downloadCouponRedisWithEvent(User user, Long couponId) {
        downLoadCoupon.setCouponDownloadWithRedisAndEvent(user, couponId);
    }


    // 판매자 쿠폰 등록
    public void uploadCoupon(User user, IssueCouponRequest issueCouponRequest) {
        user.checkRole(Role.SELLER);

        Coupon coupon = Coupon.builder()
            .seller(user)
            .discountCost(issueCouponRequest.discount())
            .quantity(issueCouponRequest.quantity())
            .expirationDate(issueCouponRequest.expirationDate())
            .build();

        couponRepository.save(coupon);
    }


    // [사용자] 사용자가 다운로드한 쿠폰 확인
    @Transactional(readOnly = true)
    public List<CouponResponse> listUserReceivedCoupons(User user, int page, int size,
        Boolean isUsed) {
        PageRequest pageRequest = PageRequest.of(page, size, DEFAULT_SORT);

        List<UserReceivedCoupon> receivedCoupons = (isUsed != null)
            ? userReceivedCouponRepository.findByUserAndIsUsed(user, pageRequest, isUsed)
            : userReceivedCouponRepository.findByUser(user, pageRequest);

        return receivedCoupons.stream()
            .filter(UserReceivedCoupon::isActive)
            .map(coupon -> new CouponResponse(coupon.getIsUsed(), toCouponInfo(coupon.getCoupon())))
            .toList();
    }

    // [사용자] 쿠폰이 사용가능한 상품 조회 (판매자가 일치하는 쿠폰만)
    // TODO: 애플리케이션 로직으로 join 실행 2025-03-4, 14:25
    /*
     * 현재 동작
     * 1. productIds를 통해 판매자 리스트 가져오기
     * 2. UserReceivedCoupon을 모두 조회한  메모리에서 필터링
     * 3. sellersId와 Coupon의 sellerId를 메모리에서 조인.
     *
     * 문제점
     * 1. N+1 문제: productIds 개수만큼 개별 쿼리 발생 (findById 호출).
     * 2. 메모리 부하: UserReceivedCoupon 전체를 메모리로 가져와 필터링.
     * 3. 조인 비효율성: sellersId.contains()는 리스트 검색(O(n))으로, 데이터가 많을수록 성능 저하.
     * */
    @Transactional(readOnly = true)
    public List<CouponPossibleOrderResponse> getUsableCouponsForProducts(User user,
        List<Long> productIds) {
        // 1. productIds를 통해 판매자 리스트 가져오기\

        // todo HashMap
        // TODO: set or hashset 2025-03-4, 15:26
        List<Long> sellersId = productIds.stream()
            .map(productId ->
                productRepository.findById(productId)
                    .orElseThrow(
                        () -> new CommonException(BError.NOT_EXIST, String.valueOf(productId))))
            .map(Product::getSeller)
            .map(User::getId)
            .toList();

        // 2. 사용하지 않은 쿠폰 중 유효기간이 남아 있는 것 필터링
        List<UserReceivedCoupon> validCoupons = userReceivedCouponRepository.findAllByUserAndIsUsed(
                user, false).stream()
            .filter(UserReceivedCoupon::isActive)
            .toList();

        // 3. 판매자가 일치하는 쿠폰만 선택하여 변환
        return validCoupons.stream()
            .filter(c -> sellersId.contains(c.getCoupon().getSeller().getId()))
            .sorted(Comparator.comparing(c -> c.getCoupon().getExpirationDate()))
            .map(c -> new CouponPossibleOrderResponse(
                c.getId(),
                c.getCoupon().getDiscountCost(),
                c.getCoupon().getExpirationDate())
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CouponPossibleOrderResponse> getUsableCouponsForProductsTobe(User user,
        List<Long> productIds) {
        // 1. productIds를 통해 판매자 리스트 가져오기\

        // todo HashMap
        // TODO: set or hashset 2025-03-4, 15:26

        Set<Long> sellersId = productRepository.findSellerIdByProductIds(productIds);

        // 2. 사용하지 않은 쿠폰 중 유효기간이 남아 있는 것 필터링
        List<UserReceivedCoupon> validCoupons = userReceivedCouponRepository.findAllByUserAndIsUsed(
                user, false).stream()
            .filter(UserReceivedCoupon::isActive)
            .toList();

        // 3. 판매자가 일치하는 쿠폰만 선택하여 변환
        return validCoupons.stream()
            .filter(c -> sellersId.contains(c.getCoupon().getSeller().getId()))
            .sorted(Comparator.comparing(c -> c.getCoupon().getExpirationDate()))
            .map(c -> new CouponPossibleOrderResponse(
                c.getId(),
                c.getCoupon().getDiscountCost(),
                c.getCoupon().getExpirationDate())
            )
            .toList();
    }

    // [사용자] 사용자가 다운로드 가능한 쿠폰 리스트 조회
    @Transactional(readOnly = true)
    public Page<ActiveCouponResponse> listDownloadableCouponsForUser(User user, Pageable pageable) {
        Page<Coupon> activeCoupons = couponRepository.findAllActiveCouponList(pageable);
        return activeCoupons.map(coupon -> createActiveCouponResponse(user, coupon));
    }

    @Transactional
    public void decreaseCouponQuantity(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));
        Coupon decreased = coupon.decreaseQuantity();
        couponRepository.save(decreased);
    }

    private ActiveCouponResponse createActiveCouponResponse(User user, Coupon coupon) {
        boolean isRegistered = isCouponRegistered(user, coupon);
        CouponInfo couponInfo = toCouponInfo(coupon);
        return new ActiveCouponResponse(isRegistered, couponInfo);
    }

    private boolean isCouponRegistered(User user, Coupon coupon) {
        return user != null && userReceivedCouponRepository.existsByUserAndCoupon(user, coupon);
    }


    private CouponInfo toCouponInfo(Coupon coupon) {
        return CouponInfo.from(
            coupon.getId(),
            coupon.getDiscountCost(),
            coupon.getExpirationDate(),
            SellerInfo.from(coupon.getSeller().getId(), coupon.getSeller().getNickname())
        );
    }

    public void couponOutOfStock(Coupon coupon) {
        Coupon OutOfStockCoupon = coupon.outOfStock();
        couponRepository.save(OutOfStockCoupon);
    }
}

