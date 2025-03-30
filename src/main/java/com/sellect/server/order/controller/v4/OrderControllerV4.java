package com.sellect.server.order.controller.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.application.v4.OrderServiceV4_1;
import com.sellect.server.order.controller.response.PaymentUrlRetrieveResponse;
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

    private final OrderServiceV4_1 orderService;
    private final UserRepository userRepository; // for test

    @PostMapping("/order/payment/{orderId}/ready")
    public ApiResponse<Void> readyPayment(
        @AuthUser User user,
        @PathVariable Long orderId,
        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {
        orderService.prepareOrder(user.getId(), orderId, userReceivedCouponId);
        return ApiResponse.ok();
    }

    @GetMapping("/order/payment/{orderId}/redirect-url")
    public ApiResponse<PaymentUrlRetrieveResponse> retrievePaymentUrl(
        @AuthUser User user,
        @PathVariable Long orderId) {
        String paymentUrl = orderService.getPaymentUrl(user, orderId);
        return ApiResponse.ok(PaymentUrlRetrieveResponse.builder()
            .paymentUrl(paymentUrl)
            .urlRetrieved(paymentUrl == null ? Boolean.FALSE : Boolean.TRUE)
            .build());
    }

    // === test 용 === //

    @PostMapping("/test/order/payment/{orderId}/ready/{userId}")
    public ApiResponse<Void> readyPayment(
        @PathVariable Long userId,
        @PathVariable Long orderId,
        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {
        orderService.prepareOrder(userId, orderId, userReceivedCouponId);
        return ApiResponse.ok();
    }

    @GetMapping("/test/order/payment/{orderId}/redirect-url/{userId}")
    public ApiResponse<PaymentUrlRetrieveResponse> retrievePaymentUrl(
        @PathVariable Long userId,
        @PathVariable Long orderId) {
        // for test
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        String paymentUrl = orderService.getPaymentUrl(user, orderId);
        return ApiResponse.ok(PaymentUrlRetrieveResponse.builder()
            .paymentUrl(paymentUrl)
            .urlRetrieved(paymentUrl == null ? Boolean.FALSE : Boolean.TRUE)
            .build());
    }
}
