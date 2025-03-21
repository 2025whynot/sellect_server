package com.sellect.server.order.controller.v0;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.application.OrderService;
import com.sellect.server.order.application.v0.OrderServiceV0After;
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
 * Order V0 부하 테스트용 컨트롤러
 * */
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v0/test")
@RestController
public class OrderV0ScenarioTestController {

    private final OrderServiceV0After orderService;
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
     * 결제요청 테스트용 api
     * */
    @PostMapping("/order/payment/{orderId}/ready/{userId}")
    public ApiResponse<String> readyPaymentTest(@PathVariable Long orderId,
        @PathVariable Long userId,
        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {

        log.info("[V0] ready!!");
        User user = User.builder()
            .id(userId)
            .build();

//        String redirectionUrl = orderService.payOrder(user, orderId, userReceivedCouponId);
//        return ApiResponse.ok(redirectionUrl);

        String pid = orderService.payOrder(user, orderId, userReceivedCouponId);
        // 테스트를 위해 pid 리턴
        return ApiResponse.ok(pid);
    }


    /***
     * 결제승인 테스트용 API
     * */
    @GetMapping("/kakao-pay/success/{pid}")
    public ApiResponse<String> approvePayment(
        @PathVariable String pid,
        @RequestParam("pg_token") String token) {

        long threadId = Thread.currentThread().getId();

        log.info("{} - [V0] approve", threadId);
        orderService.approvePayment(Long.valueOf(pid), token);
        log.info("{} - [V0] success", threadId);
        return ApiResponse.ok(pid + "success");
    }
}
