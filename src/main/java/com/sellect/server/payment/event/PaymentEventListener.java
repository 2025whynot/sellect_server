package com.sellect.server.payment.event;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentEventListener {

    private final KakaoPayClient kakaoPayClient;
    private final PaymentRepository paymentRepository;

    @Async("preparePaymentExecutor")
    @EventListener
    public void kakaoPayReadyEvent(KakaoPayReadyEvent event) {
        try {
            long pid = TsidCreator.getTsid().toLong();// UUID 대신 TSID 기반 ID 생성
            KakaoPayReadyResponse response = requestKakaoPayReady(pid, event);
            createAndSavePayment(event, pid, response);
            event.getFuture().complete(response.next_redirect_pc_url());
        } catch (Exception e) {
            event.getFuture().completeExceptionally(e);
        }
    }

    // TODO: 보상 트랜잭션  2025-03-5, 16:29
    @Async("approvePaymentExecutor")
    @EventListener
    public void kakaoPayApproveEvent(KakaoPayApproveEvent event) {

        // Transactional 보장이 안되기에 메서드로 분리한 거 하나로!
        Payment approvePayment = event.getPayment().approvePayment();
        paymentRepository.save(approvePayment);

        requestKakaoPayApporve(event, approvePayment);
    }

    public void createAndSavePayment(KakaoPayReadyEvent event, Long pid,
        KakaoPayReadyResponse response) {
        Payment payment = Payment.ready(
            event.getOrders().getId(),
            pid,
//            event.getUser().getUuid(),
            event.getUser().getId(),
            event.getOrders().getTotalPrice().intValue(),
            response.tid()
        );
        paymentRepository.save(payment);
    }

    private void requestKakaoPayApporve(KakaoPayApproveEvent event, Payment approvePayment) {
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(approvePayment.getTid())
            .partnerOrderId(String.valueOf(approvePayment.getOrdersId()))
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

    private KakaoPayReadyResponse requestKakaoPayReady(Long pid, final KakaoPayReadyEvent event) {
        Integer quantity = 0;
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequest(
            String.valueOf(event.getOrders().getId()),
//            event.getUser().getUuid(),
            String.valueOf(event.getUser().getId()),
            "test",
            quantity,
            event.getOrders().getTotalPrice().intValue(),
            String.valueOf(pid)
        );
        return kakaoPayClient.readyPayment(request);
    }

}
