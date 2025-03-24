package com.sellect.server.coupon.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    ReentrantLock lock = new ReentrantLock();
    private static final Sort DEFAULT_SORT = Sort.by(Direction.DESC, "createdAt");

    private final PlatformTransactionManager transactionManager;

    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

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

    // TODO: 애플리케이션 락 vs DB 락 vs 큐 성능측정 필요 2025-02-18, 17:7
    /*
     * 유저 쿠폰 등록기능
     * 쿠폰 수량 삭감 - 동시성 이슈 발생
     * ReentrantLock을 사용하여 해결 -> 애플리케이션에서 해결
     * 단일 인스턴스인 경우 가능한 부분
     * 스케일 아웃을 하면?? -> DB 락????
     * */


    /// 문제있는 코드
    ///
    @Transactional
    public void downloadCoupon(User user, Long couponId) {
        lock.lock();
//        log.info("[Lock]");
        try {
            Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));

            coupon.isUsable();

            if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
                throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
            }

            Coupon decreasedCoupon = coupon.decreaseQuantity();
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
                decreasedCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
            couponRepository.save(decreasedCoupon);
        } finally {
            lock.unlock();
//            log.info("[UnLock]");
        }
    }

    public void downloadCouponv2(User user, Long couponId) {
        lock.lock();
//        log.info("[Lock]");
        try {
            TransactionStatus status = transactionManager.getTransaction(
                new DefaultTransactionDefinition());
            try {
                Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(
                        () -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));

                coupon.isUsable();
                if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
                    throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
                }
                Coupon decreasedCoupon = coupon.decreaseQuantity();
                UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
                    decreasedCoupon);
                userReceivedCouponRepository.save(userReceivedCoupon);
                couponRepository.save(decreasedCoupon);

                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
                throw e;
            }
        } finally {
            lock.unlock();
//            log.info("[UnLock]");
        }
    }


    // 2. DB락
    @Transactional
    public void downloadCouponWithPessimisticLock(User user, Long couponId) {
        //        log.info("[Coupon download] 비관락");
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));
        coupon.isUsable();
        if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
        }
        Coupon decreasedCoupon = coupon.decreaseQuantity();
        UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
            decreasedCoupon);
        userReceivedCouponRepository.save(userReceivedCoupon);
        couponRepository.save(decreasedCoupon);
    }

    // 3. 분산락
    @Transactional
    public void downloadCouponWithDistributeLock(User user, Long couponId) {
        //        log.info("분산락 잠금");
        String lockKey = "couponLock:" + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        // 락 획득 시도: 최대 5초 대기, 락 유지 시간 2초
        try {
            boolean isLock = lock.tryLock(5, 2, TimeUnit.SECONDS);
            if (!isLock) {
                throw new CommonException(BError.LOCK_ACQUISITION_FAILED, couponId.toString());
            }
            Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId).orElseThrow();
            coupon.isUsable();
            if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
                throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
            }
            Coupon decreasedCoupon = coupon.decreaseQuantity();
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
                decreasedCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
            couponRepository.save(decreasedCoupon);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }


    // [사용자] 쿠폰 확인
    @Transactional(readOnly = true)
    public List<CouponResponse> getCouponList(User user, int page, int size, Boolean isUsed) {
        PageRequest pageRequest = PageRequest.of(page, size, DEFAULT_SORT);

        List<UserReceivedCoupon> receivedCoupons = (isUsed != null)
            ? userReceivedCouponRepository.findByUserAndIsUsed(user, pageRequest, isUsed)
            : userReceivedCouponRepository.findByUser(user, pageRequest);

        return receivedCoupons.stream()
            .filter(UserReceivedCoupon::isActive)
            .map(this::toCouponResponse)
            .toList();
    }

    // [사용자] 쿠폰 사용
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
    public List<CouponPossibleOrderResponse> getCouponsByMatchingSeller(User user,
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


    // [사용자] 등록한 쿠폰 리스트 조회
    @Transactional(readOnly = true)
    public Page<ActiveCouponResponse> getActiveCouponList(User user, Pageable pageable) {
        Page<Coupon> activeCoupons = couponRepository.findAllActiveCouponList(pageable);
        return activeCoupons.map(coupon -> createActiveCouponResponse(user, coupon));
    }

    private ActiveCouponResponse createActiveCouponResponse(User user, Coupon coupon) {
        boolean isRegistered = isCouponRegistered(user, coupon);
        CouponInfo couponInfo = toCouponInfo(coupon);
        return new ActiveCouponResponse(isRegistered, couponInfo);
    }

    private boolean isCouponRegistered(User user, Coupon coupon) {
        return user != null && userReceivedCouponRepository.existsByUserAndCoupon(user, coupon);
    }

    private CouponResponse toCouponResponse(UserReceivedCoupon coupon) {
        return new CouponResponse(coupon.getIsUsed(), toCouponInfo(coupon.getCoupon()));
    }

    private CouponInfo toCouponInfo(Coupon coupon) {
        return CouponInfo.from(
            coupon.getId(),
            coupon.getDiscountCost(),
            coupon.getExpirationDate(),
            SellerInfo.from(coupon.getSeller().getId(), coupon.getSeller().getNickname())
        );
    }
}

