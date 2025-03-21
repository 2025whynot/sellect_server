package com.sellect.server.payment.application;

import com.github.f4b6a3.tsid.TsidCreator;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceV0 {

    private final PaymentRepository paymentRepository;
    private final KakaoPayClient kakaoPayClient;

    public void readyPayment(User user, Long orderId, Long pid, Orders order, String tid) {
        Payment payment = Payment.ready(orderId,
            pid,
//            user.getUuid(),
            user.getId(),
            order.getTotalPrice().intValue(),
            tid);

        paymentRepository.save(payment);
    }

    public String getKakaoPayReadyResponse(User user, Long orderId, Orders order) {
        Long generatePid = generatePid();
        Integer quantity = 0;
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequestV0(
            String.valueOf(orderId),
            user.getId(),
            "test",
            quantity,
            order.getTotalPrice().intValue(),
            generatePid
        );

        KakaoPayReadyResponse kakaoPayReadyResponse = kakaoPayClient.readyPayment(request);
        readyPayment(user, orderId, generatePid, order, kakaoPayReadyResponse.tid());
        return kakaoPayReadyResponse.next_redirect_pc_url();
    }

    public Payment findReadyPaymentByPid(Long pid) {
        return paymentRepository.findByPid(pid)
            .orElseThrow(
                () -> new CommonException(BError.NOT_EXIST, String.format("Payment %s", pid)));
    }

    public void paymentApprove(Long pid, String token, Payment payment) {
        Payment approvePayment = payment.approve();
        paymentRepository.save(approvePayment);

        // 카카오 한테 요청 보내기
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(payment.getTid())
            .partnerOrderId(String.valueOf(payment.getOrdersId()))
//            .partnerUserId(payment.getUid())
            .partnerUserId(String.valueOf(payment.getUserId()))
            .pgToken(token)
            .build();

        KakaoPayApproveResponse kakaoPayApproveResponse = kakaoPayClient.paymentApprove(approveRequest);
        log.info("Payment approved for pid: {}", pid);
    }

    private Long generatePid() {
        return TsidCreator.getTsid().toLong(); // UUID 대신 TSID 기반 ID 생성
    }
}
