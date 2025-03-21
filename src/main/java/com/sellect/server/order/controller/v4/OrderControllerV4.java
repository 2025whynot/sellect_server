package com.sellect.server.order.controller.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.application.v4.OrderServiceV4;
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
@RequestMapping("/api/v4")
public class OrderControllerV4 {

    private final OrderServiceV4 orderService;

    @PostMapping("/order/payment/{orderId}/ready")
    public ApiResponse<Void> readyPayment(@AuthUser User user, @PathVariable Long orderId,
        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {
        orderService.preparePayment(user, orderId, userReceivedCouponId);
        return ApiResponse.ok();
    }

    @GetMapping("/order/payment/{orderId}/ready")
    public ApiResponse<String> getPaymentUrl(@AuthUser User user, @PathVariable Long orderId) {
        String paymentUrl = orderService.getPaymentUrl(user, orderId);
        return ApiResponse.ok(paymentUrl);
    }
}
