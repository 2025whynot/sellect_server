package com.sellect.server.order.Infrastructure.port;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FakePayClient implements PayClient {

    // todo: 성능 테스트 시 변경
    private static final String FAKE_PAYMENT_HOST = "http://localhost:8081";

    @Value("${server.host}")
    private String SERVER_HOST;
    private static final RestTemplate restTemplate = new RestTemplate();

    @Override
    public KakaoPayReadyResponse readyPayment(KakaoPayReadyRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<KakaoPayReadyRequest> readyRequest = new HttpEntity<>(request, headers);
        ResponseEntity<KakaoPayReadyResponse> response = restTemplate.exchange(
            FAKE_PAYMENT_HOST + "/v1/payment/ready", HttpMethod.POST, readyRequest, KakaoPayReadyResponse.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new CommonException(BError.KAKAO_READY_FAIL);
        }

        KakaoPayReadyResponse readyResponse = response.getBody();
        return readyResponse;
    }

    @Override
    public KakaoPayApproveResponse paymentApprove(ApproveRequest approveRequest) {
        try {
            String approveUrl = FAKE_PAYMENT_HOST + "/v1/payment/approve";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // ApproveRequest를 Map으로 변환
            Map<String, String> body = new HashMap<>();
            body.put("tid", approveRequest.tid());
            body.put("orderId", approveRequest.partnerOrderId());
            body.put("userId", approveRequest.partnerUserId());
            body.put("pg_token", approveRequest.pgToken());

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                approveUrl, HttpMethod.POST, requestEntity, Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("Approve 실패: status={}, body={}", response.getStatusCode(), response.getBody());
                throw new CommonException(BError.KAKAO_APPROVE_FAIL, "Fake approve 실패");
            }

            Map<String, String> responseBody = response.getBody();
            log.info("Approve 호출 성공: tid={}, orderId={}", approveRequest.tid(), approveRequest.partnerOrderId());

            // KakaoPayApproveResponse로 변환
            return new KakaoPayApproveResponse(
                "FAKE_AID_" + UUID.randomUUID().toString().substring(0, 8), // aid
                approveRequest.tid(),
                "TC0ONETIME",
                approveRequest.partnerOrderId(),
                approveRequest.partnerUserId(),
                "CARD", // methodType 예시
                "Fake Item",
                1,
                null,
                LocalDateTime.now(),
                LocalDateTime.parse(responseBody.get("approved_at")), // 컨트롤러에서 받은 시간
                null
            );
        } catch (Exception e) {
            log.error("Approve 호출 실패: tid={}, error={}", approveRequest.tid(), e.getMessage());
            throw new CommonException(BError.PAYMENT_FAILED, "Approve 호출 실패: " + e.getMessage());
        }
    }

    @Override
    public KakaoPayReadyRequest createKakaoPayReadyRequest(String partnerOrderId, String partnerUserId, String itemName, Integer quantity, Integer totalAmount, String pid) {
        return KakaoPayReadyRequest.builder()
            .cid("TC0ONETIME")
            .partnerOrderId(partnerOrderId)
            .partnerUserId(partnerUserId)
            .itemName(itemName)
            .quantity(quantity)
            .totalAmount(totalAmount)
            .taxFreeAmount(0)
            .approvalUrl(String.format("%s/api/v1/kakao-pay/success/%s", SERVER_HOST, pid))
            .cancelUrl(String.format("%s/api/v1/kakao-pay/cancel", SERVER_HOST))
            .failUrl(String.format("%s/api/v1/kakao-pay/fail", SERVER_HOST))
            .build();
    }

// before
//    public static void triggerInProgress(String tid, String approvalUrl) {
//        try {
//            String pid = approvalUrl.substring(approvalUrl.lastIndexOf("/") + 1);
//            String inProgressUrl = FAKE_PAYMENT_HOST + "/v1/payment/in-progress";
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            Map<String, String> body = new HashMap<>();
//            body.put("tid", tid);
//            body.put("pid", pid);
//            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
//
//            restTemplate.postForEntity(inProgressUrl, requestEntity, Void.class);
//            log.info("In-progress 호출 성공: tid={}, pid={}", tid, pid);
//        } catch (Exception e) {
//            log.error("In-progress 호출 실패: tid={}, approvalUrl={}", tid, approvalUrl, e);
//            throw new CommonException(BError.PAYMENT_FAILED, "In-progress 호출 실패: " + e.getMessage());
//        }
//    }
// after
    public static void triggerInProgressTest(Long pid) {
        try {
            String tid = "tid - " + pid.toString();
            String inProgressUrl = FAKE_PAYMENT_HOST + "/v1/payment/in-progress";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = new HashMap<>();
            body.put("tid", tid);
            body.put("pid", pid.toString());
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(inProgressUrl, requestEntity, Void.class);
            log.info("In-progress 호출 성공 : tid={}, pid={}", tid, pid);
        } catch (Exception e) {
            throw new CommonException(BError.PAYMENT_FAILED, "In-progress 호출 실패: " + e.getMessage());
        }
    }
}


// 백업용
//package com.sellect.server.order.Infrastructure.port;
//
//import com.sellect.server.common.exception.CommonException;
//import com.sellect.server.common.exception.enums.BError;
//import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
//import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
//import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
//import com.sellect.server.payment.controller.request.ApproveRequest;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FakePayClient implements PayClient {
//
//    // todo: 성능 테스트 시 변경
//    private static final String FAKE_PAYMENT_HOST = "http://localhost:8081";
//
//    @Value("${server.host}")
//    private String SERVER_HOST;
//    private static final RestTemplate restTemplate = new RestTemplate();
//
//    @Override
//    public KakaoPayReadyResponse readyPayment(KakaoPayReadyRequest request) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<KakaoPayReadyRequest> readyRequest = new HttpEntity<>(request, headers);
//        ResponseEntity<KakaoPayReadyResponse> response = restTemplate.exchange(
//            FAKE_PAYMENT_HOST + "/v1/payment/ready", HttpMethod.POST, readyRequest, KakaoPayReadyResponse.class);
//
//        if (response.getStatusCode() != HttpStatus.OK) {
//            throw new CommonException(BError.KAKAO_READY_FAIL);
//        }
//
//        KakaoPayReadyResponse readyResponse = response.getBody();
//        return readyResponse;
//    }
//
//    @Override
//    public KakaoPayApproveResponse paymentApprove(ApproveRequest approveRequest) {
//        try {
//            String approveUrl = FAKE_PAYMENT_HOST + "/v1/payment/approve";
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            // ApproveRequest를 Map으로 변환
//            Map<String, String> body = new HashMap<>();
//            body.put("tid", approveRequest.tid());
//            body.put("orderId", approveRequest.partnerOrderId());
//            body.put("userId", approveRequest.partnerUserId());
//            body.put("pg_token", approveRequest.pgToken());
//
//            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
//            ResponseEntity<Map> response = restTemplate.exchange(
//                approveUrl, HttpMethod.POST, requestEntity, Map.class
//            );
//
//            if (response.getStatusCode() != HttpStatus.OK) {
//                log.error("Approve 실패: status={}, body={}", response.getStatusCode(), response.getBody());
//                throw new CommonException(BError.KAKAO_APPROVE_FAIL, "Fake approve 실패");
//            }
//
//            Map<String, String> responseBody = response.getBody();
//            log.info("Approve 호출 성공: tid={}, orderId={}", approveRequest.tid(), approveRequest.partnerOrderId());
//
//            // KakaoPayApproveResponse로 변환
//            return new KakaoPayApproveResponse(
//                "FAKE_AID_" + UUID.randomUUID().toString().substring(0, 8), // aid
//                approveRequest.tid(),
//                "TC0ONETIME",
//                approveRequest.partnerOrderId(),
//                approveRequest.partnerUserId(),
//                "CARD", // methodType 예시
//                "Fake Item",
//                1,
//                null,
//                LocalDateTime.now(),
//                LocalDateTime.parse(responseBody.get("approved_at")), // 컨트롤러에서 받은 시간
//                null
//            );
//        } catch (Exception e) {
//            log.error("Approve 호출 실패: tid={}, error={}", approveRequest.tid(), e.getMessage());
//            throw new CommonException(BError.PAYMENT_FAILED, "Approve 호출 실패: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public KakaoPayReadyRequest createKakaoPayReadyRequest(String partnerOrderId, String partnerUserId, String itemName, Integer quantity, Integer totalAmount, String pid) {
//        return KakaoPayReadyRequest.builder()
//            .cid("TC0ONETIME")
//            .partnerOrderId(partnerOrderId)
//            .partnerUserId(partnerUserId)
//            .itemName(itemName)
//            .quantity(quantity)
//            .totalAmount(totalAmount)
//            .taxFreeAmount(0)
//            .approvalUrl(String.format("%s/api/v1/kakao-pay/success/%s", SERVER_HOST, pid))
//            .cancelUrl(String.format("%s/api/v1/kakao-pay/cancel", SERVER_HOST))
//            .failUrl(String.format("%s/api/v1/kakao-pay/fail", SERVER_HOST))
//            .build();
//    }
//
//    public static void triggerInProgress(String tid, String approvalUrl) {
//        try {
//            String pid = approvalUrl.substring(approvalUrl.lastIndexOf("/") + 1);
//            String inProgressUrl = FAKE_PAYMENT_HOST + "/v1/payment/in-progress";
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            Map<String, String> body = new HashMap<>();
//            body.put("tid", tid);
//            body.put("pid", pid);
//            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
//
//            restTemplate.postForEntity(inProgressUrl, requestEntity, Void.class);
//            log.info("In-progress 호출 성공: tid={}, pid={}", tid, pid);
//        } catch (Exception e) {
//            log.error("In-progress 호출 실패: tid={}, approvalUrl={}", tid, approvalUrl, e);
//            throw new CommonException(BError.PAYMENT_FAILED, "In-progress 호출 실패: " + e.getMessage());
//        }
//    }
//}
