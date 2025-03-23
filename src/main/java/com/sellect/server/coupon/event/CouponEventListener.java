package com.sellect.server.coupon.event;


import com.sellect.server.coupon.application.CouponService;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CouponEventListener {

    private final CouponService couponService;

    private final BlockingQueue<CouponDownloadEvent> eventQueue = new LinkedBlockingQueue<>();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponDownloadEvent(CouponDownloadEvent event) {
        eventQueue.add(event);
//        couponService.decreaseCouponQuantity(event.getCouponId());
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
