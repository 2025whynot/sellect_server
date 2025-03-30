package com.sellect.server.payment.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.event.message.OrderCompleteMessage;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository; // TODO: 테스트용
    private final KafkaProducer kafkaProducer; // TODO: 테스트용

    @GetMapping("/payment/history")
    public ApiResponse<List<PaymentHistoryResponse>> getPaymentHistory(
        @AuthUser User user,
        @PageableDefault(page = 0, size = 5, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {
        List<PaymentHistoryResponse> paymentHistory = paymentService.getPaymentHistory(user,
            pageable);
        return ApiResponse.ok(paymentHistory);
    }

    // === test 용 === //
    @GetMapping("/test/payment/kakao-pay/success/{pid}")
    public ApiResponse<String> approvePayment(
        @PathVariable String pid,
        @RequestParam(value = "pg_token", defaultValue = "test") String token) {

        log.info("pid: {}, token: {}", pid, token);
        Payment payment = paymentRepository.findByPid(Long.valueOf(pid))
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "payment"));

        kafkaProducer.produce("order-complete",
            OrderCompleteMessage.builder()
                .orderId(payment.getOrdersId())
                .pid(Long.valueOf(pid))
                .token(token)
                .payment(payment)
                .build());
        return ApiResponse.ok(pid + "success");
    }

}
