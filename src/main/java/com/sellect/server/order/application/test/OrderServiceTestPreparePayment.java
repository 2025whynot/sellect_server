package com.sellect.server.order.application.test;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderStatus;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceTestPreparePayment {

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final PlatformTransactionManager transactionManager;


    public long preparePayment(User user, Long orderId) {

        // 트랜잭션 정의 및 시작
        TransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(definition);
        Orders order;
        Payment payment;
        long pid;
        try {
            order = ordersRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "PENDING ORDER"));

            // 유저의 주문인지 확인
            order.validateOwner(user);

            pid = TsidCreator.getTsid().toLong();
            payment = createAndSavePreparedPayment(user, order, pid); // payment 저장 (READY) [항상은 아님]

            transactionManager.commit(status);
        } catch (CommonException e) {
            transactionManager.rollback(status);
            throw e;
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new CommonException(BError.INTERNAL_SERVER_ERROR,
                "preparePayment() - 결제 준비 중 오류 발생");
        }
        // 트랜잭션 커밋 후 이벤트 발행
//        CompletableFuture<KakaoPayReadyResponse> future = new CompletableFuture<>();
//        KakaoPayReadyEvent kakaoPayReadyEvent = new KakaoPayReadyEvent(this, user, order, null);
//        eventPublisher.publishEvent(kakaoPayReadyEvent);
        return payment.getPid();
    }

    private Payment createAndSavePreparedPayment(User user, Orders orders, Long pid) {
        try {
            Payment payment = Payment.ready(
                orders.getId(),
                pid,
                user.getId(),
                orders.getTotalPrice().intValue(),
                "test tid: - " + orders.getId().toString()
            );
            return paymentRepository.save(payment);
        } catch (DataAccessException e) {
//            log.error("결제 정보 저장 실패: orderId={}, pid={}", event.getOrders().getId(), pid, e);
            throw new CommonException(BError.DB_ERROR, "결제 정보 저장 실패: " + e.getMessage());
        }
    }

}
