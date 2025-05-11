package com.sellect.server.payment.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.controller.response.PaymentUrlRetrieveResponse;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository; // for test

    @GetMapping("/payment/{orderId}/payment-url")
    public ApiResponse<PaymentUrlRetrieveResponse> retrievePaymentUrl(
            @AuthUser User user,
            @PathVariable Long orderId) {
        String paymentUrl = paymentService.getPaymentUrl(user, orderId);
        return ApiResponse.ok(PaymentUrlRetrieveResponse.builder()
                .paymentUrl(paymentUrl)
                .urlRetrieved(paymentUrl == null ? Boolean.FALSE : Boolean.TRUE)
                .build());
    }

    @GetMapping("/payment/history")
    public ApiResponse<List<PaymentHistoryResponse>> getPaymentHistory(
        @AuthUser User user,
        @PageableDefault(page = 0, size = 5, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {
        List<PaymentHistoryResponse> paymentHistory = paymentService.getPaymentHistory(user,
            pageable);
        return ApiResponse.ok(paymentHistory);
    }

    @GetMapping("/payment/kakao-pay/success/{pid}")
    public ApiResponse<String> approvePayment(
        @PathVariable String pid,
        @RequestParam(value = "pg_token") String token) {
        paymentService.initPaymentApproval(Long.valueOf(pid), token);
        return ApiResponse.ok(pid + "success");
    }

    // === test 용 === //
    @GetMapping("/test/payment/kakao-pay/success/{pid}")
    public ApiResponse<String> approvePaymentForTest(
        @PathVariable String pid,
        @RequestParam(value = "pg_token", defaultValue = "test") String token) {
        paymentService.initPaymentApproval(Long.valueOf(pid), token);
        return ApiResponse.ok(pid + "success");
    }

    @GetMapping("/test/payment/{orderId}/payment-url/{userId}")
    public ApiResponse<PaymentUrlRetrieveResponse> retrievePaymentUrl(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        // for test
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));
        String paymentUrl = paymentService.getPaymentUrl(user, orderId);
        return ApiResponse.ok(PaymentUrlRetrieveResponse.builder()
                .paymentUrl(paymentUrl)
                .urlRetrieved(paymentUrl == null ? Boolean.FALSE : Boolean.TRUE)
                .build());
    }

}
