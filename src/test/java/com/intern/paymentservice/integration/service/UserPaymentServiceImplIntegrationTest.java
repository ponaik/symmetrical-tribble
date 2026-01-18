package com.intern.paymentservice.integration.service;

import com.intern.paymentservice.TestcontainersConfiguration;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.exception.UserAccessDeniedException;
import com.intern.paymentservice.integration.NoSecurityConfig;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.AuthenticationService;
import com.intern.paymentservice.service.impl.UserPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Import({TestcontainersConfiguration.class, NoSecurityConfig.class})
@ActiveProfiles("test")
@SpringBootTest
@Tag("integration")
class UserPaymentServiceImplIntegrationTest {

    @Autowired
    UserPaymentServiceImpl userPaymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @MockitoBean
    AuthenticationService authenticationService;

    private static final Long AUTH_USER_ID = 101L;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        // Stub the internal ID for the "logged in" user context
        given(authenticationService.getInternalId()).willReturn(AUTH_USER_ID);
    }

    @Test
    void givenUserCreatingOwnPayment_whenCreatePayment_thenPaymentIsPersistedAsPending() {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest(10L, AUTH_USER_ID, BigDecimal.TEN);

        // when
        PaymentResponse response = userPaymentService.createPayment(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(AUTH_USER_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

        // Verify DB persistence
        assertThat(paymentRepository.findById(response.id())).isPresent();
    }

    @Test
    void givenUserCreatingPaymentForOthers_whenCreatePayment_thenThrowAccessDenied() {
        // given
        Long otherUserId = 999L;
        CreatePaymentRequest request = new CreatePaymentRequest(10L, otherUserId, BigDecimal.TEN);

        // when / then
        assertThatThrownBy(() -> userPaymentService.createPayment(request))
                .isInstanceOf(UserAccessDeniedException.class)
                .hasMessageContaining(String.valueOf(AUTH_USER_ID));
    }

    @Test
    void givenUserFetchingOwnPayments_whenFindPaymentsByUserId_thenReturnsList() {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest(10L, AUTH_USER_ID, BigDecimal.TEN);
        PaymentResponse created = userPaymentService.createPayment(request);

        // when
        List<PaymentResponse> results = userPaymentService.findPaymentsByUserId(AUTH_USER_ID);

        // then
        assertThat(results)
                .isNotEmpty()
                .hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(created.id());
    }

    @Test
    void givenUserFetchingOthersPayments_whenFindPaymentsByUserId_thenThrowAccessDenied() {
        // given
        Long otherUserId = 999L;

        // when / then
        assertThatThrownBy(() -> userPaymentService.findPaymentsByUserId(otherUserId))
                .isInstanceOf(UserAccessDeniedException.class);
    }

    @Test
    void givenUserDeletingOthersPayment_whenDeletePayment_thenPaymentIsNotDeleted() {
        // given
        // 1. Create a payment belonging to a DIFFERENT user
        Payment payment = Payment.builder()
                .userId(999L)
                .build();
        Payment otherPayment = paymentRepository.save(payment);


        // when
        userPaymentService.deletePayment(otherPayment.getId());

        // then
        // Verify the payment still exists (Silent failure expected by implementation)
        Optional<Payment> check = paymentRepository.findById(otherPayment.getId());
        assertThat(check).isPresent();
    }
}