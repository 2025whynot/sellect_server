package com.sellect.server.coupon.repository;

import com.sellect.server.coupon.domain.Coupon;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FakeCouponRepository implements CouponRepository {

    private final Map<Long, Coupon> storage = new ConcurrentHashMap<>();
    private Long id = 1L;

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            coupon = Coupon.builder()
                .id(id++)
                .seller(coupon.getSeller())
                .discountCost(coupon.getDiscountCost())
                .quantity(coupon.getQuantity())
                .expirationDate(coupon.getExpirationDate())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .deleteAt(coupon.getDeleteAt())
                .build();
        }
        storage.put(coupon.getId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(storage.get(couponId));
    }

    @Override
    public Page<Coupon> findAllActiveCouponList(Pageable pageable) {
        LocalDate now = LocalDate.now();

        // 1. 필터링된 쿠폰 리스트 생성
        List<Coupon> activeCoupons = storage.values().stream()
            .filter(coupon -> coupon.getDeleteAt() == null)           // 삭제되지 않은 쿠폰
            .filter(coupon -> coupon.getQuantity() > 0)              // 수량이 남아 있는 쿠폰
            .filter(coupon -> coupon.getExpirationDate().isAfter(now)) // 만료되지 않은 쿠폰
            .sorted(Comparator.comparing(Coupon::getCreatedAt).reversed()) // 최신 등록순
            .toList();

        // 2. 페이지네이션 적용
        int start = (int) pageable.getOffset(); // 시작 인덱스
        int end = Math.min(start + pageable.getPageSize(), activeCoupons.size()); // 끝 인덱스
        List<Coupon> pagedCoupons = (start < activeCoupons.size())
            ? activeCoupons.subList(start, end)
            : Collections.emptyList();

        // 3. Page 객체로 반환
        return new PageImpl<>(pagedCoupons, pageable, activeCoupons.size());
    }

    @Override
    public Optional<Coupon> findByIdWithPessimisticLock(Long couponId) {
        return Optional.empty();
    }
}
