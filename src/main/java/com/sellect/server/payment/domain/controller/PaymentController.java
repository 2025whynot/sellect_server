package com.sellect.server.payment.domain.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.payment.domain.controller.application.PaymentService;
import com.sellect.server.payment.domain.controller.request.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ready")
    public ResponseEntity<?> initialPayment(@AuthUser User user,
        @RequestBody PaymentRequest request) {
        String redirectUrl = paymentService.initialPayment(user, request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        log.info(redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/success/{pid}")
    public String approvePayment(
        @PathVariable String pid,
        @RequestParam("pg_token") String token) {
        paymentService.approvePayment(pid, token);

        // todo: home redirect
        return "success";
    }

    @GetMapping("/cancel")
    public String cancelPayment() {
        return "cancel";
    }

    @GetMapping("/fail")
    public String failPayment() {
        return "fail";
    }
}
