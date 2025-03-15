package com.sellect.server.payment.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceV0 {

    private final PaymentRepository paymentRepository;
    private final KakaoPayClient kakaoPayClient;

    public void readyPayment(User user, Long orderId, String pid, Orders order, String tid) {
        Payment payment = Payment.ready(String.valueOf(orderId),
            pid,
            user.getUuid(),
            order.getTotalPrice().intValue(),
            tid);

        paymentRepository.save(payment);
    }

    public String getKakaoPayReadyResponse(User user, Long orderId, Orders order) {
        String pid = generatePaymentId();
        Integer quantity = 0;
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequestV0(
            String.valueOf(orderId),
            user.getUuid(),
            "test",
            quantity,
            order.getTotalPrice().intValue(),
            pid
        );

        KakaoPayReadyResponse kakaoPayReadyResponse = kakaoPayClient.readyPayment(request);
        readyPayment(user, orderId, pid, order, kakaoPayReadyResponse.tid());
        return kakaoPayReadyResponse.next_redirect_pc_url();
    }

    private String generatePaymentId() {
        return String.valueOf(UUID.randomUUID());
    }

    public Payment findReadyPaymentByPid(String pid) {
        return paymentRepository.findByPid(pid)
            .orElseThrow(
                () -> new CommonException(BError.NOT_EXIST, String.format("Payment %s", pid)));
    }

    public void paymentApprove(String pid, String token, Payment payment) {
        Payment approvePayment = payment.approvePayment();
        paymentRepository.save(approvePayment);

        // todo: 카카오 요청까지 하는 처리 속도 -> 락 범위 최소화 -> 이는 테스트 시나리오로 가능
        // todo: 테스트 시나리오를 통해 확인할 것!
        // thread.sleep(50)으로 해서 하면 되지 않을까? 굳이 계속 kakao 요청을 보내는 것보다는 나을 듯
        // 테스트를 해야하기에
        // 카카오 한테 요청 보내기
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(payment.getTid())
            .partnerOrderId(payment.getOrderId())
            .partnerUserId(payment.getUid())
            .pgToken(token)
            .build();

        KakaoPayApproveResponse kakaoPayApproveResponse = kakaoPayClient.paymentApprove(approveRequest);
        log.info("Payment approved for pid: {}", pid);
    }
}
