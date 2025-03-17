package com.sellect.server.payment.application;

import com.sellect.server.auth.domain.User;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(User user, Pageable pageable) {
//        Page<Payment> paymentHistoryByUser = paymentRepository.findPaymentHistoryByUser(user.getUuid(), pageable);
        Page<Payment> paymentHistoryByUser = paymentRepository.findPaymentHistoryByUser(user.getId(), pageable);

        return paymentHistoryByUser.getContent().stream()
            .map(PaymentHistoryResponse::of)
            .toList();
    }

}

