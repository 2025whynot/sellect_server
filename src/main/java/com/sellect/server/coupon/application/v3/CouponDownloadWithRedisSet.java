package com.sellect.server.coupon.application.v3;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.application.CouponService;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.event.MemberCouponRemoveEvent;
import com.sellect.server.coupon.infra.MemberCouponStockOperation;
import com.sellect.server.coupon.repository.CouponRepository;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.product.repository.ProductRepository;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CouponDownloadWithRedisSet extends CouponService {

    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final MemberCouponStockOperation memberCouponStockOperation;
    private final ApplicationEventPublisher eventPublisher;

    public CouponDownloadWithRedisSet(CouponRepository couponRepository,
        UserReceivedCouponRepository userReceivedCouponRepository,
        ProductRepository productRepository, CouponRepository couponRepository1,
        UserReceivedCouponRepository userReceivedCouponRepository1,
        ProductRepository productRepository1, RedissonClient redissonClient,
        MemberCouponStockOperation memberCouponStockOperation,
        ApplicationEventPublisher eventPublisher) {
        super(couponRepository, userReceivedCouponRepository, productRepository);
        this.couponRepository = couponRepository1;
        this.userReceivedCouponRepository = userReceivedCouponRepository1;
        this.productRepository = productRepository1;
        this.redissonClient = redissonClient;
        this.memberCouponStockOperation = memberCouponStockOperation;
        this.eventPublisher = eventPublisher;
    }

    /**
      * 방향성
      * 쿠폰을 수량만큼 삭제가 아닌, 재고에 대한 사용량을 증가하는 형식으로 수정
      *  시퀀스
      * 1. 트랜잭션 시작
      * 2. 현재 다운로드가 가능한지 유효성 검사
      * 3. 다운이 가능한 경우 Redis에 추가
      * 4. RDB의 user_received_coupon에 추가
      * 5. 트랜잭션 커밋
      * 참고
      * https://techblog.woowahan.com/2709/
      */
    @Transactional
    @Override
    public void downloadCoupon(User user, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, couponId.toString()));

        // 인당 재고 확인
        memberCouponStockOperation.add(couponId, user);
        try {
            int totalUsedCount = memberCouponStockOperation.totalUsedCount(couponId, user);
            if (totalUsedCount > coupon.getQuantity()) {
                throw new CommonException(BError.COUPON_QUANTITY_ZERO, couponId.toString());
            }
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
        } catch (Exception e) {
            // 실패시 Redis 롤백 (보상 트랜잭션)
            eventPublisher.publishEvent(new MemberCouponRemoveEvent(couponId, user.getId()));
            throw e;
        }
    }
}
