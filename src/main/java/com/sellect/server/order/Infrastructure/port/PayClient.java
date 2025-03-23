package com.sellect.server.order.Infrastructure.port;

import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;

public interface PayClient {
    // 최대한 기존 카카오와 유사하게 하기 위해 request, response 는 그대로 사용
    KakaoPayReadyResponse readyPayment(KakaoPayReadyRequest request);
    KakaoPayApproveResponse paymentApprove(ApproveRequest approveRequest);
    KakaoPayReadyRequest createKakaoPayReadyRequest(String partnerOrderId, String partnerUserId, String itemName, Integer quantity, Integer totalAmount, String pid);
}
