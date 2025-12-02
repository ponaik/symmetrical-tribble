package com.intern.paymentservice.integration.service;

import com.intern.paymentservice.PaymentServiceApplication;
import com.intern.paymentservice.TestcontainersConfiguration;
import com.intern.paymentservice.client.PaymentResultClient;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.integration.NoSecurityConfig;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.PaymentFacade;
import com.intern.paymentservice.service.broker.PaymentProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.intern.paymentservice.service.AuthenticationService.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@Import({TestcontainersConfiguration.class, NoSecurityConfig.class})
@ActiveProfiles("test")
@SpringBootTest(classes = PaymentServiceApplication.class)
@Tag("integration")
class PaymentFacadeAdminFlowIntegrationTest {

    @Autowired
    PaymentFacade paymentFacade;

    @Autowired
    PaymentRepository paymentRepository;

    @MockitoBean
    PaymentResultClient paymentResultClient;

    @MockitoBean
    PaymentProducer paymentProducer;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        given(paymentResultClient.getPaymentResult()).willReturn(200);

        doNothing().when(paymentProducer).sendPaymentUpdate(any());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenAdminUser_whenCreatePayment_thenPaymentIsPersistedAndStatusIsUpdated() {
        // given
        Long orderId = 101L;
        Long userId = 1001L;
        CreatePaymentRequest request = new CreatePaymentRequest(orderId, userId, BigDecimal.valueOf(100.00));

        // when
        PaymentResponse response = paymentFacade.createPayment(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

        // Verify Persistence
        Optional<Payment> saved = paymentRepository.findById(response.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenExistingPayment_whenUpdatePaymentStatus_thenStatusIsUpdated() {
        // given
        Payment payment = Payment.builder()
                .orderId(202L)
                .userId(1002L)
                .paymentAmount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();
        payment = paymentRepository.save(payment);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.FAILED);

        // when
        PaymentResponse response = paymentFacade.updatePaymentStatus(payment.getId(), request);

        // then
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenExistingPayment_whenDeletePayment_thenPaymentIsRemoved() {
        // given
        Payment payment = Payment.builder()
                .orderId(303L)
                .userId(1003L)
                .paymentAmount(BigDecimal.ONE)
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();
        payment = paymentRepository.save(payment);

        // when
        paymentFacade.deletePayment(payment.getId());

        // then
        Optional<Payment> deleted = paymentRepository.findById(payment.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenPaymentsForDifferentOrders_whenFindPaymentsByOrderId_thenReturnsCorrectList() {
        // given
        Long targetOrderId = 404L;
        paymentRepository.save(Payment.builder().orderId(targetOrderId).userId(1L).paymentAmount(BigDecimal.TEN).build());
        paymentRepository.save(Payment.builder().orderId(targetOrderId).userId(2L).paymentAmount(BigDecimal.TEN).build());
        paymentRepository.save(Payment.builder().orderId(999L).userId(3L).paymentAmount(BigDecimal.TEN).build());

        // when
        List<PaymentResponse> results = paymentFacade.findPaymentsByOrderId(targetOrderId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(p -> p.orderId().equals(targetOrderId));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenPaymentsForDifferentUsers_whenFindPaymentsByUserId_thenReturnsCorrectList() {
        // given
        Long targetUserId = 505L;
        // Admin should be able to see ANY user's payments
        paymentRepository.save(Payment.builder().userId(targetUserId).orderId(1L).paymentAmount(BigDecimal.TEN).build());
        paymentRepository.save(Payment.builder().userId(targetUserId).orderId(2L).paymentAmount(BigDecimal.TEN).build());
        paymentRepository.save(Payment.builder().userId(888L).orderId(3L).paymentAmount(BigDecimal.TEN).build());

        // when
        List<PaymentResponse> results = paymentFacade.findPaymentsByUserId(targetUserId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(p -> p.userId().equals(targetUserId));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenPaymentsWithDifferentStatuses_whenFindPaymentsByStatuses_thenReturnsFilteredList() {
        // given
        paymentRepository.save(Payment.builder().status(PaymentStatus.PENDING).build());
        paymentRepository.save(Payment.builder().status(PaymentStatus.FAILED).build());
        paymentRepository.save(Payment.builder().status(PaymentStatus.SUCCESS).build());

        // when
        List<PaymentResponse> results = paymentFacade.findPaymentsByStatuses(
                List.of(PaymentStatus.PENDING, PaymentStatus.FAILED));

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(PaymentResponse::status)
                .containsExactlyInAnyOrder(PaymentStatus.PENDING, PaymentStatus.FAILED);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenPaymentsInTimeRange_whenFindPaymentTotalForPeriod_thenReturnsSum() {
        // given
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant twoHoursAgo = now.minusSeconds(7200);

        // In range
        paymentRepository.save(Payment.builder().timestamp(now).paymentAmount(BigDecimal.valueOf(10.00)).build());
        paymentRepository.save(Payment.builder().timestamp(oneHourAgo).paymentAmount(BigDecimal.valueOf(20.00)).build());
        
        // Out of range
        paymentRepository.save(Payment.builder().timestamp(twoHoursAgo).paymentAmount(BigDecimal.valueOf(50.00)).build());

        // when
        PaymentTotalResponse total = paymentFacade.findPaymentTotalForPeriod(
                now.minusSeconds(4000), // Start: slightly more than 1 hour ago
                now.plusSeconds(60)     // End: now
        );

        // then (10 + 20 = 30)
        assertThat(total.paymentTotal()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
    }
}