package com.sellect.server.order.controller.v1;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.Infrastructure.port.FakePayClient;
import com.sellect.server.order.application.OrderService;
import com.sellect.server.order.application.test.OrderServiceTestPreparePayment;
import com.sellect.server.order.application.v1.OrderServiceV1After;
import com.sellect.server.order.controller.request.OrderAddRequest;
import com.sellect.server.order.controller.response.PendingOrderRegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/***
 * Order V1 부하 테스트용 컨트롤러
 * */
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/test")
@RestController
public class OrderV1ScenarioTestController {

    private final OrderServiceTestPreparePayment OrderServiceTestPreparePayment;
    private final OrderServiceV1After orderService;
    private final OrderService orignalOrderService;

    /**
     * 주문 생성(pending)
     */
    @PostMapping("/order/pending/{userId}")
    public ApiResponse<PendingOrderRegisterResponse> registerPendingOrder(@PathVariable Long userId,
        @Valid @RequestBody OrderAddRequest requests) {
        User user = User.builder()
            .id(userId)
            .build();
        PendingOrderRegisterResponse response = orignalOrderService.registerPendingOrder(user,
            requests);
        return ApiResponse.ok(response);
    }


    /***
     * orderId, userId
     * */
    @PostMapping("/order/payment/{orderId}/ready/{userId}")
    public ApiResponse<String> readyPaymentTest(@PathVariable Long orderId, @PathVariable Long userId) {

        log.info("[V1] ready!!");
        User user = User.builder()
            .id(userId)
            .build();

        // before
        // KakaoPayReadyResponse kakaoPayReadyResponse = orderService.preparePayment(user,
        //    Long.valueOf(orderId));
        // [TODO: 성능 테스트를 위해서 어쩔 수 없이 추가했어야 함. 배포시 삭제해야함]
        // FakePayClient.triggerInProgress(kakaoPayReadyResponse.tid(), kakaoPayReadyResponse.next_redirect_pc_url());
        // return ApiResponse.ok(kakaoPayReadyResponse.next_redirect_pc_url());

        // after
        long result = OrderServiceTestPreparePayment.preparePayment(user, orderId);
        // String 으로  pid 반환
        return ApiResponse.ok(String.valueOf(result));
    }


    @PostMapping("/order/payment/in-progress/{pid}")
    public ApiResponse<Boolean> inProgressPaymentTest(@PathVariable Long pid) {

        FakePayClient.triggerInProgressTest(pid);
        return ApiResponse.ok(true);
    }

    /***
     * 결제승인 테스트용 API
     * */
    @GetMapping("/kakao-pay/success/{pid}")
    public ApiResponse<String> approvePayment(
        @PathVariable String pid,
        @RequestParam(value = "pg_token", defaultValue = "test") String token) {

        long threadId = Thread.currentThread().getId();
        log.info("{} - [V1] approve", threadId);
        orderService.approvePayment(Long.valueOf(pid), token);
        log.info("{} - [V1] success", threadId);
        return ApiResponse.ok(pid + "success");
    }
}


// 백업용
//package com.sellect.server.order.controller.v1;
//
//import com.sellect.server.auth.domain.User;
//import com.sellect.server.common.response.ApiResponse;
//import com.sellect.server.order.Infrastructure.port.FakePayClient;
//import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
//import com.sellect.server.order.application.OrderService;
//import com.sellect.server.order.application.v1.OrderServiceV1After;
//import com.sellect.server.order.controller.request.OrderAddRequest;
//import com.sellect.server.order.controller.response.PendingOrderRegisterResponse;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//
///***
// * Order V1 부하 테스트용 컨트롤러
// * */
//@RequiredArgsConstructor
//@Slf4j
//@RequestMapping("/api/v1/test")
//@RestController
//public class OrderV1ScenarioTestController {
//
//    private final OrderServiceV1After orderService;
//    private final OrderService orignalOrderService;
//
//    /**
//     * 주문 생성(pending)
//     */
//    @PostMapping("/order/pending/{userId}")
//    public ApiResponse<PendingOrderRegisterResponse> registerPendingOrder(@PathVariable Long userId,
//        @Valid @RequestBody OrderAddRequest requests) {
//        User user = User.builder()
//            .id(userId)
//            .build();
//        PendingOrderRegisterResponse response = orignalOrderService.registerPendingOrder(user,
//            requests);
//        return ApiResponse.ok(response);
//    }
//
//
//    /***
//     * 결제요청 테스트용 api
//     * */
//    @PostMapping("/order/payment/{orderId}/ready/{userId}")
//    public ApiResponse<String> readyPaymentTest(@PathVariable String orderId,
//        @PathVariable Long userId) {
//
//        log.info("[V1] ready!!");
//        User user = User.builder()
//            .id(userId)
//            .build();
//
//        KakaoPayReadyResponse kakaoPayReadyResponse = orderService.preparePayment(user,
//            Long.valueOf(orderId));
//        // [TODO: 성능 테스트를 위해서 어쩔 수 없이 추가했어야 함. 배포시 삭제해야함]
//        FakePayClient.triggerInProgress(kakaoPayReadyResponse.tid(), kakaoPayReadyResponse.next_redirect_pc_url());
//        return ApiResponse.ok(kakaoPayReadyResponse.next_redirect_pc_url());
////        String pid = orderService.preparePayment(user, orderId);
////        return ApiResponse.ok(pid);
//    }
//
//
//    /***
//     * 결제승인 테스트용 API
//     * */
//    @GetMapping("/kakao-pay/success/{pid}")
//    public ApiResponse<String> approvePayment(
//        @PathVariable String pid,
//        @RequestParam("pg_token") String token) {
//
//        long threadId = Thread.currentThread().getId();
//        log.info("{} - [V1] approve", threadId);
//        orderService.approvePayment(Long.valueOf(pid), token);
//        log.info("{} - [V1] success", threadId);
//        return ApiResponse.ok(pid + "success");
//    }
//}