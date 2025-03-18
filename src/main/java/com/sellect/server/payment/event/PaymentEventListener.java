package com.sellect.server.payment.event;

import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class PaymentEventListener {

    private final KakaoPayClient kakaoPayClient;
    private final PaymentRepository paymentRepository;

    @Async("paymentTaskExecutor")
    @EventListener
    public void kakaoPayReadyEvent(KakaoPayReadyEvent event) {
        try {
            String pid = String.valueOf(UUID.randomUUID());
            KakaoPayReadyResponse response = requestKakaoPayReady(pid, event);
            createAndSavePayment(event, pid, response);
            event.getFuture().complete(response.next_redirect_pc_url());
        } catch (Exception e) {
            event.getFuture().completeExceptionally(e);
        }
    }

    //tx2
    // TODO: 보상 트랜잭션  2025-03-5, 16:29
    @Async("paymentTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void kakaoPayApproveEvent(KakaoPayApproveEvent event) {
        Payment approvePayment = approveAndSavePayment(event);
        requestKakaoPayApporve(event, approvePayment);
    }

    public void createAndSavePayment(KakaoPayReadyEvent event, String pid,
        KakaoPayReadyResponse response) {
        Payment payment = Payment.ready(
            String.valueOf(event.getOrderId()),
            pid,
//            event.getUser().getUuid(),
            event.getUser().getId(),
            event.getOrder().getTotalPrice().intValue(),
            response.tid()
        );
        paymentRepository.save(payment);
    }

    @Transactional
    public Payment approveAndSavePayment(KakaoPayApproveEvent event) {
        Payment approvePayment = event.getPayment().approvePayment();
        paymentRepository.save(approvePayment);
        return approvePayment;
    }

    private void requestKakaoPayApporve(KakaoPayApproveEvent event, Payment approvePayment) {
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(approvePayment.getTid())
            .partnerOrderId(approvePayment.getOrderId())
//            .partnerUserId(approvePayment.getUid())
            .partnerUserId(String.valueOf(approvePayment.getUserId()))
            .pgToken(event.getToken())
            .build();

        //터졋어..
        KakaoPayApproveResponse kakaoPayApproveResponse = kakaoPayClient.paymentApprove(
            approveRequest);

        //재시도

        //복귀(saga 패턴)
        //이벤트 발생

    }

    private KakaoPayReadyResponse requestKakaoPayReady(String pid, KakaoPayReadyEvent event) {
        Integer quantity = 0;
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequest(
            String.valueOf(event.getOrderId()),
//            event.getUser().getUuid(),
            String.valueOf(event.getUser().getId()),
            "test",
            quantity,
            event.getOrder().getTotalPrice().intValue(),
            pid
        );
        return kakaoPayClient.readyPayment(request);
    }

}
