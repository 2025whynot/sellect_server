package com.sellect.server.payment.repository;

import static org.assertj.core.api.BDDAssertions.then;

import com.sellect.server.config.JpaConfig;
import com.sellect.server.config.JsonConfig;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.entity.PaymentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({
    JpaConfig.class,
    JsonConfig.class,
})
class PaymentRepositoryImplTest {
    @Autowired
    private TestEntityManager em;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;
    private PaymentRepositoryImpl paymentRepository;

    @BeforeEach
    void setUp(){
        paymentRepository = new PaymentRepositoryImpl(paymentJpaRepository);
    }

    @Nested
    @DisplayName("save()")
    class PaymentSaveTest{
        @Test
        @DisplayName("[성공] payment를 저장한다.")
        void willSuccess() {
            //given
            Payment payment = Payment.ready(1234L, 123L, 1L, 1000, "tid");

            //when
            paymentRepository.save(payment);

            //then
            PaymentEntity test = paymentJpaRepository.findByPid(123L).orElseThrow();
            then(test.getTid()).isEqualTo("tid");
            then(test.getPid()).isEqualTo(123L);
        }
    }

    @Nested
    @DisplayName("findByPid()")
    class PaymentFindByPidTest{
        @Test
        @DisplayName("[성공]")
        void willSuccess() {
            //given
            Payment payment = Payment.ready(1234L, 123L, 1L, 1000, "tid");
            PaymentEntity save = paymentJpaRepository.save(PaymentEntity.from(payment));

            //when
            Payment foundPayment = paymentRepository.findByPid(123L).orElseThrow();

            //then
            then(foundPayment.getPid()).isEqualTo(save.getPid());
            then(foundPayment.getId()).isEqualTo(save.getId());

        }
    }


    @Nested
    @DisplayName("findPaymentHistoryByUser()")
    class PaymentFindPaymentHistoryByUserTest{
        @Test
        @DisplayName("[성공]")
        void willSuccess() {
            //given
            Long userId = 1L;
            Payment payment1 = Payment.ready(1L, 123L, userId, 1000, "tid");
            Payment payment2 = Payment.ready(22L, 124L, userId, 2000, "tid");
            Payment payment3 = Payment.ready(333L, 125L, userId, 3000, "tid");
            PaymentEntity save1 = paymentJpaRepository.save(PaymentEntity.from(payment1));
            PaymentEntity save2 = paymentJpaRepository.save(PaymentEntity.from(payment2));
            PaymentEntity save3 = paymentJpaRepository.save(PaymentEntity.from(payment3));
            PageRequest pageable = PageRequest.of(0, 5);

            //when
//            Page<Payment> paymentHistory = paymentRepository.findPaymentHistoryByUser("uid", pageable);
            Page<Payment> paymentHistory = paymentRepository.findPaymentHistoryByUser(userId, pageable);

            //then
            then(paymentHistory).isNotNull();
            then(paymentHistory).hasSize(3);
            then(paymentHistory).allMatch(payment -> payment.getUserId().equals(userId));
            then(paymentHistory).extracting(Payment::getOrdersId).containsExactlyInAnyOrder(1L, 22L, 333L);
            then(paymentHistory).extracting(Payment::getPrice).containsExactlyInAnyOrder(1000, 2000, 3000);

        }
    }
}
