package com.sellect.server.payment.application;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.port.PayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.KakaoPayApproveEvent;
import com.sellect.server.payment.event.KakaoPayReadyEvent;
import com.sellect.server.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceV1 {

    private final PayClient payClient;
    private final PaymentRepository paymentRepository;

    public KakaoPayReadyResponse preparePayment(KakaoPayReadyEvent event) {
        long pid = TsidCreator.getTsid().toLong();
        int retryCount = 0;
        boolean success = false; // 재시도 trigger
        String errorMsg = null;

        KakaoPayReadyResponse response = null;
        // 재시도 로직 [최대 1회]
        while (retryCount <= 1 && !success) {
            log.info("카카오페이 API 시도: retryCount={}, orderId={}", retryCount, event.getOrders().getId());
            try {
                response = requestKakaoPayReady(pid, event);
                createAndSavePreparedPayment(event, pid, response); // payment 저장 (READY) [항상은 아님]
                success = true;
                log.info("카카오페이 API 완료: retryCount={}, orderId={}", retryCount, event.getOrders().getId());
            } catch (ResourceAccessException | HttpServerErrorException e) { // 재시도 가능한 예외: 네트워크 오류, 서버 오류
                retryCount++;
                errorMsg = e.getMessage();
                if (retryCount <= 1) {
                    sleepForRetry();
                }
            } catch (HttpClientErrorException e) { // 재시도 불필요: 클라이언트 오류
                errorMsg = "카카오페이 준비 요청 오류: " + e.getMessage();
                throw new CommonException(BError.PAYMENT_FAILED, errorMsg);
            } catch (Exception e) { // 기타 예외: 즉시 실패 처리
                errorMsg = e.getMessage();
                throw new CommonException(BError.PAYMENT_FAILED, "결제 준비 중 오류: " + errorMsg);
            }
        }

        if (!success) {
            // 재시도 실패 시
            throw new CommonException(BError.PAYMENT_FAILED, "카카오페이 결제 준비 실패: " + errorMsg);
        }
        return response;
    }

    public void approvePayment(final KakaoPayApproveEvent event) {
        int retryCount = 0;
        boolean success = false;
        String errorMsg = null;

        Payment approvePayment = saveApprovePayment(event);

        while (retryCount <= 1 && !success) {
            log.info("카카오페이 승인 요청 시도: retryCount={}, pid={}", retryCount, event.getPid());
            try {
                requestKakaoPayApporve(event, approvePayment);
                success = true;
            } catch (ResourceAccessException | HttpServerErrorException e) {
                retryCount++;
                errorMsg = e.getMessage();
                if (retryCount <= 1) {
                    sleepForRetry();
                }
            } catch (HttpClientErrorException e) {
                errorMsg = "카카오페이 승인 요청 오류: " + e.getMessage();
                throw new CommonException(BError.PAYMENT_FAILED, errorMsg);
            } catch (Exception e) {
                errorMsg = e.getMessage();
                throw new CommonException(BError.PAYMENT_FAILED, "결제 승인 중 오류: " + errorMsg);
            }
        }

        if (!success) {
            throw new CommonException(BError.PAYMENT_FAILED, "카카오페이 결제 승인 실패: " + errorMsg);
        }
    }


    private Payment saveApprovePayment(final KakaoPayApproveEvent event) {
        try {
            Payment approvePayment = event.getPayment().approve();
            return paymentRepository.save(approvePayment);
        } catch (DataAccessException e) {
            log.error("결제 승인 상태 저장 실패: pid={}", event.getPid(), e);
            throw new CommonException(BError.DB_ERROR, "결제 승인 상태 저장 실패");
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


    private void sleepForRetry() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "재시도 중 인터럽트: " + ie.getMessage());
        }
    }
}
