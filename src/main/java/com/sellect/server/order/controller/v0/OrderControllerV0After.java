package com.sellect.server.order.controller.v0;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.order.application.v0.OrderServiceV0After;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/after")
public class OrderControllerV0After {
    private final OrderServiceV0After orderService;

    // 리팩터링 전(이벤트 기반 리팩터링 전)
    // 주문 생성(pending 상태) -> 결제 요청(카카오페이 api, redirect URL을 넘겨받음) -> redirectURL을
    @PostMapping("/order/payment/{orderId}/ready")
    public ApiResponse<String> readyPayment(@AuthUser User user, @PathVariable Long orderId,
        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {

        log.info("[V0] ready!!");
        KakaoPayReadyResponse kakaoPayReadyResponse = orderService.payOrder(user, orderId,
            userReceivedCouponId);
        return ApiResponse.ok(kakaoPayReadyResponse.next_redirect_pc_url());
    }

    // 테스트를 위해서 approve를 위해 이곳에 api url 설정
    @GetMapping("/kakao-pay/success/{pid}")
    public ApiResponse<String> approvePayment(
        @PathVariable String pid,
        @RequestParam("pg_token") String token) {

        Long longPid = convertToLong(pid);

        long threadId = Thread.currentThread().getId();
        log.info("{} - [V0] approve", threadId);

        orderService.approvePayment(longPid, token);

        log.info("{} - [V0] success", threadId);
        return ApiResponse.ok(pid + "success");
    }

    @GetMapping("/kakao-pay/cancel/{pid}")
    public ApiResponse<Object> cancelPayment(
        @PathVariable String pid
    ) {
//        paymentService.cancelPayment(pid);
        return ApiResponse.ok();
    }

    @GetMapping("/kakao-pay/fail/{pid}")
    public ApiResponse<Object> failPayment(
        @PathVariable String pid
    ) {
//        paymentService.failPayment(pid);
        return ApiResponse.ok();
    }

    private Long convertToLong(String pid) {
        try {
            return Long.parseLong(pid);
        } catch (NumberFormatException e) {
            log.error("Invalid pid format: {}", pid);
            throw new CommonException(BError.NOT_VALID, "Invalid pid format: " + pid);
        }
    }
}
