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
import com.sellect.server.coupon.event.CouponDownloadEvent;
import com.sellect.server.coupon.event.MemberCouponRemoveEvent;
import com.sellect.server.coupon.infra.CouponStockOperation;
import com.sellect.server.coupon.infra.MemberCouponStockOperation;
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
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final CouponStockOperation couponStockOperation;
    private final MemberCouponStockOperation memberCouponStockOperation;

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

    @Transactional
    public void downloadCoupon(User user, Long couponId) {
        lock.lock();
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
        }
    }

    public void downloadCouponv2(User user, Long couponId) {
        lock.lock();
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
        }
    }


    // 2. DB락
    @Transactional
    public void downloadCouponWithPessimisticLock(User user, Long couponId) {
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
    public void downloadCouponWithDistributeLock(User user, Long couponId) {
        String lockKey = String.format("coupon:couponLock:%d", couponId);
        RLock lock = redissonClient.getLock(lockKey);

        // 락 획득 시도: 최대 5초 대기, 락 유지 시간 2초
        try {
            boolean isLock = lock.tryLock(5, 2, TimeUnit.SECONDS);
            if (!isLock) {
                throw new CommonException(BError.LOCK_ACQUISITION_FAILED, couponId.toString());
            }
            Coupon coupon = couponRepository.findById(couponId).orElseThrow();
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


    // 문제
    // 만약 쿠폰 수량이랑 DB에서 찾을때 안맞으면???
    @Transactional
    public void downloadCouponWithRedis(User user, Long couponId) {
        String counterKey = "coupon:" + couponId + ":count";
        String userCouponKey = "coupon:" + couponId + ":users";
        RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
        RSet<String> userSet = redissonClient.getSet(userCouponKey);

        if (!counter.isExists()) {
            Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, couponId.toString()));
            counter.set(coupon.getQuantity());
        }

        // Redis에서 수량 감소
        long remaining = counter.get();
        if (remaining <= 0) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO, couponId.toString());
        }
        counter.decrementAndGet();
        try {
            // 수량 체크
            Coupon coupon = couponRepository.findById(couponId).orElseThrow(() ->
                new CommonException(BError.NOT_EXIST, couponId.toString()));

            // 중복 체크
            String userIdStr = user.getId().toString();
            if (!userSet.add(userIdStr)) {
                counter.incrementAndGet(); // 롤백
                throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
            }

            // 사용자 쿠폰 저장
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
            // 이벤트 발행
            eventPublisher.publishEvent(new CouponDownloadEvent(couponId));
        } catch (Exception e) {
            // 트랜잭션 롤백 시 Redis도 롤백
            counter.incrementAndGet();
            userSet.remove(user.getId().toString());
            throw e;
        }
    }

    // 방향성
    // 쿠폰을 수량만큼 삭제가 아닌, 재고에 대한 사용량을 증가하는 형식으로 수정
    // 시퀀스
    // 1. 트랜잭션 시작
    // 2. 현재 다운로드가 가능한지 유효성 검사
    // 3. 다운이 가능한 경우 Redis에 redis에 add
    // 4. rdb의 user recceived coupon에 추가
    // 5. 트랜잭션 커밋
    // https://techblog.woowahan.com/2709/

    @Transactional
    public void downloadCouponWithRedisV2(User user, Long couponId) {
        // 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, couponId.toString()));

        // 인당 제고 확인
        memberCouponStockOperation.add(couponId, user);
        int totalUsedCount = memberCouponStockOperation.totalUsedCount(couponId, user);
        if (totalUsedCount > coupon.getQuantity()) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO, couponId.toString());
        }
        try {
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
        } catch (Exception e) {
            eventPublisher.publishEvent(new MemberCouponRemoveEvent(couponId, user.getId()));
            throw e;
        }
    }

    // [사용자] 사용자가 다운로드한 쿠폰 확인
    @Transactional(readOnly = true)
    public List<CouponResponse> listUserReceivedCoupons(User user, int page, int size, Boolean isUsed) {
        PageRequest pageRequest = PageRequest.of(page, size, DEFAULT_SORT);

        List<UserReceivedCoupon> receivedCoupons = (isUsed != null)
            ? userReceivedCouponRepository.findByUserAndIsUsed(user, pageRequest, isUsed)
            : userReceivedCouponRepository.findByUser(user, pageRequest);

        return receivedCoupons.stream()
            .filter(UserReceivedCoupon::isActive)
            .map(this::toCouponResponse)
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


    // [사용자] 등록한 쿠폰 리스트 조회
    @Transactional(readOnly = true)
    public Page<ActiveCouponResponse> getActiveCouponList(User user, Pageable pageable) {
        Page<Coupon> activeCoupons = couponRepository.findAllActiveCouponList(pageable);
        return activeCoupons.map(coupon -> createActiveCouponResponse(user, coupon));
    }

    @Transactional
    public void decreaseCouponQuantity(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));
        Coupon decreased = coupon.decreaseQuantity();
        couponRepository.save(decreased);

        log.info("Decrease coupon quantity: {}", couponId);
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

