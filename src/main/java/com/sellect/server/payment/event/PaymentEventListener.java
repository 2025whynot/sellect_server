package com.sellect.server.payment.event;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.port.PayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventListener {

    private final PayClient payClient;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async("preparePaymentExecutor")
    @EventListener
    public void kakaoPayReadyEvent(KakaoPayReadyEvent event) {
        long pid = TsidCreator.getTsid().toLong();
        int retryCount = 0;
        boolean success = false; // 재시도 trigger
        String errorMsg = null;

        // 재시도 로직 [최대 1회]
        while (retryCount <= 1 && !success) {
            log.info("카카오페이 API 시도: retryCount={}, orderId={}", retryCount, event.getOrders().getId());
            try {
                KakaoPayReadyResponse response = requestKakaoPayReady(pid, event);
                createAndSavePreparedPayment(event, pid, response); // payment 저장 (READY) [항상은 아님]

                event.getFuture().complete(response);
                success = true;
                log.info("카카오페이 API 완료: retryCount={}, orderId={}", retryCount, event.getOrders().getId());
            } catch (ResourceAccessException | HttpServerErrorException e) { // 재시도 가능한 예외: 네트워크 오류, 서버 오류
                retryCount++;
                errorMsg = e.getMessage();
                if (retryCount <= 1) {
                    try {
                        Thread.sleep(1000); // 1초 대기 후 재시도
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        errorMsg = "재시도 중 인터럽트: " + ie.getMessage();
                        break;
                    }
                }
            } catch (HttpClientErrorException e) { // 재시도 불필요: 클라이언트 오류
                errorMsg = "카카오페이 준비 요청 오류: " + e.getMessage();
                break;
            } catch (Exception e) { // 기타 예외: 즉시 실패 처리
                errorMsg = e.getMessage();
                break;
            }
        }

        if (!success) {
            // 재시도 실패 시
            event.getFuture().completeExceptionally(
                new CommonException(BError.PAYMENT_FAILED, "카카오페이 결제 준비 실패: " + errorMsg)
            );
            eventPublisher.publishEvent(
                PaymentPrepareFailedEvent.builder()
                    .orders(event.getOrders())
                    .pid(pid)
                    .reason(errorMsg)
                    .build()
            );
        }
    }

    @Async("approvePaymentExecutor")
    @EventListener
    public void kakaoPayApproveEvent(final KakaoPayApproveEvent event) {
        int retryCount = 0;
        boolean success = false;
        String errorMsg = null;

        Payment approvePayment;
        try {
            approvePayment = event.getPayment().approve();
            paymentRepository.save(approvePayment);
        } catch (DataAccessException e) {
            log.error("결제 승인 상태 저장 실패: pid={}", event.getPid(), e);
            eventPublisher.publishEvent( // 예외를 반환 전에 보상 트랜잭션 이벤트를 발행해서 주문, 재고 복구시키기
                PaymentApproveFailedEvent.builder()
                    .payment(event.getPayment())
                    .pid(event.getPid())
                    .reason("DB 저장 실패: " + e.getMessage())
                    .build()
            );
            throw new CommonException(BError.DB_ERROR, "결제 승인 상태 저장 실패");
        }

        while (retryCount <= 1 && !success) {
            log.info("카카오페이 승인 요청 시도: retryCount={}, pid={}", retryCount, event.getPid());
            try {
                requestKakaoPayApporve(event, approvePayment);
                success = true;
            } catch (ResourceAccessException | HttpServerErrorException e) {
                retryCount++;
                errorMsg = e.getMessage();
                if (retryCount <= 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        errorMsg = "재시도 중 인터럽트: " + ie.getMessage();
                        break;
                    }
                }
            } catch (HttpClientErrorException e) {
                errorMsg = "카카오페이 승인 요청 오류: " + e.getMessage();
                break;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                break;
            }
        }

        if (!success) {
            log.error("카카오페이 승인 실패: pid={}, error={}", event.getPid(), errorMsg);
            eventPublisher.publishEvent(
                PaymentApproveFailedEvent.builder()
                    .payment(approvePayment)
                    .pid(event.getPid())
                    .reason(errorMsg)
                    .build()
            );
        }
    }

    private void createAndSavePreparedPayment(KakaoPayReadyEvent event, Long pid,
        KakaoPayReadyResponse response) {
        try {
            Payment payment = Payment.ready(
                event.getOrders().getId(),
                pid,
                event.getUser().getId(),
                event.getOrders().getTotalPrice().intValue(),
                response.tid()
            );
            paymentRepository.save(payment);
        } catch (DataAccessException e) {
            log.error("결제 정보 저장 실패: orderId={}, pid={}", event.getOrders().getId(), pid, e);
            throw new CommonException(BError.DB_ERROR, "결제 정보 저장 실패: " + e.getMessage());
        }
    }

    private void requestKakaoPayApporve(KakaoPayApproveEvent event, Payment approvePayment) {
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(approvePayment.getTid())
            .partnerOrderId(String.valueOf(approvePayment.getOrdersId()))
            .partnerUserId(String.valueOf(approvePayment.getUserId()))
            .pgToken(event.getToken())
            .build();

        KakaoPayApproveResponse kakaoPayApproveResponse = payClient.paymentApprove(approveRequest);
        log.info("카카오페이 승인 완료: pid={}", event.getPid());
    }

    private KakaoPayReadyResponse requestKakaoPayReady(Long pid, final KakaoPayReadyEvent event) {
        Integer quantity = 0;
        KakaoPayReadyRequest request = payClient.createKakaoPayReadyRequest(
            String.valueOf(event.getOrders().getId()),
//            event.getUser().getUuid(),
            String.valueOf(event.getUser().getId()),
            "test",
            quantity,
            event.getOrders().getTotalPrice().intValue(),
            String.valueOf(pid)
        );
        return payClient.readyPayment(request);
    }

}
