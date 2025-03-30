package com.sellect.server.coupon.event;


import com.sellect.server.coupon.application.CouponService;
import com.sellect.server.coupon.application.v2.CouponDownloadWithRedisAndEvent;
import com.sellect.server.coupon.infra.CouponStockOperation;
import com.sellect.server.coupon.infra.MemberCouponStockOperation;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventListener {
    private final BlockingQueue<CouponDownloadEvent> eventQueue = new LinkedBlockingQueue<>();
    private final CouponStockOperation couponStockOperation;
    private final MemberCouponStockOperation memberCouponStockOperation;
    private final CouponService couponService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponDownloadEvent(CouponDownloadEvent event) {
        eventQueue.add(event);
    }

    @Async
    @EventListener
    public void handleCouponQuantityDecreaseEvent(CouponDecreaseEvent event) {
        couponStockOperation.decreaseStock(event.getCouponId());
    }

    @Async
    @EventListener
    public void handleCouponOutOfStockEvent(CouponOutOfStockEvent event) {
        couponService.couponOutOfStock(event.getCoupon());
    }

    @Async
    @EventListener
    public void handleMemberCouponRemoveEvent(MemberCouponRemoveEvent event) {
        memberCouponStockOperation.remove(event.getCouponId(), event.getUserId());
    }

    // 비동기로 큐에 담긴 이벤트를 처리
    @PostConstruct
    public void processQueue(){
        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                try {
                    CouponDownloadEvent event = eventQueue.take(); // 하나씩 꺼내서 처리
                    couponService.decreaseCouponQuantity(event.getCouponId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
}
