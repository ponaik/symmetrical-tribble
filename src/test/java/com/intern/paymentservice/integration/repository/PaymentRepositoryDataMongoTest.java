package com.intern.paymentservice.integration.repository;

import com.intern.paymentservice.TestcontainersConfiguration;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestPropertySource(properties = {"mongock.enabled=false"})
@DataMongoTest
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class PaymentRepositoryDataMongoTest {

    @Autowired
    PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void givenPayment_whenSaveAndFindByOrderIdAndUserId_thenPaymentIsReturned() {
        long userId = 100L;
        long orderId = 200L;
        Payment p = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentAmount(BigDecimal.valueOf(10))
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        paymentRepository.save(p);

        List<Payment> found = paymentRepository.findByOrderIdAndUserId(orderId, userId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(found.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void givenMultiplePaymentsForDifferentUsersAndStatuses_whenFindByStatusInAndUserId_thenOnlyMatchingPaymentsAreReturned() {
        long userId = 101L;

        Payment p1 = Payment.builder()
                .orderId(1L)
                .userId(userId)
                .status(PaymentStatus.PENDING)
                .paymentAmount(BigDecimal.ONE)
                .timestamp(Instant.now())
                .build();

        Payment p2 = Payment.builder()
                .orderId(2L)
                .userId(userId)
                .status(PaymentStatus.FAILED)
                .paymentAmount(BigDecimal.TEN)
                .timestamp(Instant.now())
                .build();

        // different user - should not be returned
        Payment pOther = Payment.builder()
                .orderId(3L)
                .userId(999L)
                .status(PaymentStatus.PENDING)
                .paymentAmount(BigDecimal.ONE)
                .timestamp(Instant.now())
                .build();

        paymentRepository.saveAll(List.of(p1, p2, pOther));

        List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING, PaymentStatus.FAILED);
        List<Payment> results = paymentRepository.findByStatusInAndUserId(statuses, userId);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Payment::getUserId).containsOnly(userId);
    }

    @Test
    void givenPaymentsInsideAndOutsidePeriod_whenFindPaymentTotalForPeriodAndUserId_thenCorrectAmountIsSummed() {
        long userId = 202L;
        Instant now = Instant.now();
        Instant start = now.minus(10, ChronoUnit.DAYS);
        Instant end = now.plus(1, ChronoUnit.DAYS);

        Payment inside1 = Payment.builder()
                .userId(userId)
                .paymentAmount(BigDecimal.valueOf(12.34))
                .timestamp(now.minus(1, ChronoUnit.DAYS))
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment inside2 = Payment.builder()
                .userId(userId)
                .paymentAmount(BigDecimal.valueOf(2.66))
                .timestamp(now)
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment outside = Payment.builder()
                .userId(userId)
                .paymentAmount(BigDecimal.valueOf(100))
                .timestamp(now.minus(30, ChronoUnit.DAYS))
                .status(PaymentStatus.SUCCESS)
                .build();

        paymentRepository.saveAll(List.of(inside1, inside2, outside));

        var total = paymentRepository.findPaymentTotalForPeriodAndUserId(start, end, userId);

        assertThat(total).isNotNull();
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(15.00));
    }

    @Test
    void givenPaymentExists_whenFindByIdAndUserId_thenOptionalWrappedPaymentIsReturned() {
        long userId = 300L;
        Payment p = Payment.builder()
                .userId(userId)
                .orderId(301L)
                .paymentAmount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        Payment saved = paymentRepository.save(p);

        // Success Case
        Optional<Payment> found = paymentRepository.findByIdAndUserId(saved.getId(), userId);
        assertThat(found).isPresent();

        // Failure Case (Correct ID, Wrong User)
        Optional<Payment> notFound = paymentRepository.findByIdAndUserId(saved.getId(), 999L);
        assertThat(notFound).isEmpty();
    }

    @Test
    void givenMultiplePaymentsWithSameOrderId_whenFindByOrderId_thenAllMatchingPaymentsAreReturned() {
        long targetOrderId = 400L;

        Payment p1 = Payment.builder()
                .orderId(targetOrderId)
                .userId(1L)
                .paymentAmount(BigDecimal.ONE)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        Payment p2 = Payment.builder()
                .orderId(targetOrderId)
                .userId(2L)
                .paymentAmount(BigDecimal.TWO)
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        Payment pOther = Payment.builder()
                .orderId(999L) // Different Order ID
                .userId(1L)
                .paymentAmount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        paymentRepository.saveAll(List.of(p1, p2, pOther));

        List<Payment> results = paymentRepository.findByOrderId(targetOrderId);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Payment::getOrderId).containsOnly(targetOrderId);
    }

    @Test
    void givenPaymentsForMultipleUsers_whenFindByUserId_thenOnlyMatchingPaymentsAreReturned() {
        long targetUserId = 500L;

        Payment p1 = Payment.builder()
                .userId(targetUserId)
                .orderId(1L)
                .paymentAmount(BigDecimal.TEN)
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        Payment pOther = Payment.builder()
                .userId(888L) // Different User
                .orderId(2L)
                .paymentAmount(BigDecimal.TEN)
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        paymentRepository.saveAll(List.of(p1, pOther));

        List<Payment> results = paymentRepository.findByUserId(targetUserId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUserId()).isEqualTo(targetUserId);
    }

    @Test
    void givenPaymentsWithDifferentStatuses_whenFindByStatusIn_thenOnlyMatchingStatusesAreReturned() {
        Payment p1 = Payment.builder()
                .status(PaymentStatus.PENDING)
                .userId(1L).orderId(1L).paymentAmount(BigDecimal.TEN).timestamp(Instant.now())
                .build();

        Payment p2 = Payment.builder()
                .status(PaymentStatus.FAILED)
                .userId(2L).orderId(2L).paymentAmount(BigDecimal.TEN).timestamp(Instant.now())
                .build();

        Payment p3 = Payment.builder()
                .status(PaymentStatus.SUCCESS)
                .userId(3L).orderId(3L).paymentAmount(BigDecimal.TEN).timestamp(Instant.now())
                .build();

        paymentRepository.saveAll(List.of(p1, p2, p3));

        List<PaymentStatus> filter = List.of(PaymentStatus.PENDING, PaymentStatus.FAILED);

        List<Payment> results = paymentRepository.findByStatusIn(filter);

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.PENDING, PaymentStatus.FAILED)
                .doesNotContain(PaymentStatus.SUCCESS);
    }
}
