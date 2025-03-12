package com.sellect.server.order.Infrastructure.port;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class KakaoPayClient {
    @Value("${kakao.pay.secret-key}")
    private String PAY_SECRET_KEY;
    @Value("${server.host}")
    private String SERVER_HOST;
    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment/ready";
    private static final String KAKAO_PAY_APPROVE_API_URL = "https://open-api.kakaopay.com/online/v1/payment/approve";
    private final RestTemplate restTemplate;

    // ready
    public KakaoPayReadyResponse readyPayment(KakaoPayReadyRequest request) {
        HttpHeaders headers = createHeaders();
        HttpEntity<KakaoPayReadyRequest> readyRequest = new HttpEntity<>(request, headers);
        ResponseEntity<KakaoPayReadyResponse> response = restTemplate.exchange(KAKAO_PAY_API_URL,
            HttpMethod.POST, readyRequest,
            KakaoPayReadyResponse.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new CommonException(BError.KAKKO_READY_FAIL);
        }

        return response.getBody();
    }

    // approve
    public KakaoPayApproveResponse paymentApprove(ApproveRequest approveRequest) {
        HttpHeaders headers = createHeaders();
        HttpEntity<ApproveRequest> request = new HttpEntity<>(approveRequest, headers);
        ResponseEntity<KakaoPayApproveResponse> response = restTemplate.exchange(KAKAO_PAY_APPROVE_API_URL,
            HttpMethod.POST, request, KakaoPayApproveResponse.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new CommonException(BError.KAKKO_APPROVE_FAIL);
        }

        return response.getBody();
    }


    public KakaoPayReadyRequest createKakaoPayReadyRequest(String partnerOrderId, String partnerUserId, String itemName, Integer quantity, Integer totalAmount, String pid) {
        return KakaoPayReadyRequest.builder()
            .cid("TC0ONETIME")
            .partnerOrderId(partnerOrderId)
            .partnerUserId(partnerUserId)
            .itemName(itemName)                 // TODO: 아이템 이름 가져오기(클라이언트에서 가져오는거 고려) 2025-02-28, 16:58
            .quantity(quantity)                 // TODO: 주문에서 아이템 개수  2025-02-28, 16:58
            .totalAmount(totalAmount)
            .taxFreeAmount(0)
            .approvalUrl(String.format("%s/api/v1/kakao-pay/success/%s", SERVER_HOST, pid))
            .cancelUrl(String.format("%s/api/v1/kakao-pay/cancel", SERVER_HOST))
            .failUrl(String.format("%s/api/v1/kakao-pay/fail", SERVER_HOST))
            .build();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "SECRET_KEY " + PAY_SECRET_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    /// 리팩터링 전 버전
    public KakaoPayReadyRequest createKakaoPayReadyRequestV0(String partnerOrderId, String partnerUserId, String itemName, Integer quantity, Integer totalAmount, String pid) {
        return KakaoPayReadyRequest.builder()
            .cid("TC0ONETIME")
            .partnerOrderId(partnerOrderId)
            .partnerUserId(partnerUserId)
            .itemName(itemName)                 // TODO: 아이템 이름 가져오기(클라이언트에서 가져오는거 고려) 2025-02-28, 16:58
            .quantity(quantity)                 // TODO: 주문에서 아이템 개수  2025-02-28, 16:58
            .totalAmount(totalAmount)
            .taxFreeAmount(0)
            .approvalUrl(String.format("%s/api/v0/kakao-pay/success/%s", SERVER_HOST, pid))
            .cancelUrl(String.format("%s/api/v0/kakao-pay/cancel", SERVER_HOST))
            .failUrl(String.format("%s/api/v0/kakao-pay/fail", SERVER_HOST))
            .build();
    }





}
