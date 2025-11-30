package com.intern.paymentservice.service.impl.unit;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.exception.PaymentNotFoundException;
import com.intern.paymentservice.exception.UserAccessDeniedException;
import com.intern.paymentservice.mapper.PaymentMapper;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.AuthenticationService;
import com.intern.paymentservice.service.impl.UserPaymentServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserPaymentServiceImplTest {

    @Mock
    AuthenticationService authenticationService;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentMapper paymentMapper;

    @InjectMocks
    UserPaymentServiceImpl service;

    @Test
    void createPayment_validRequest_returnsPaymentResponse() {
        // given
        long internalId = 42L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        CreatePaymentRequest request = new CreatePaymentRequest(1L, internalId, BigDecimal.TEN);
        Payment incoming = Payment.builder()
                .orderId(1L)
                .userId(internalId)
                .paymentAmount(BigDecimal.TEN)
                .build();
        given(paymentMapper.toEntity(request)).willReturn(incoming);

        Payment saved = Payment.builder()
                .id("payment-id")
                .orderId(1L)
                .userId(internalId)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .paymentAmount(BigDecimal.TEN)
                .build();
        given(paymentRepository.save(any(Payment.class))).willReturn(saved);

        PaymentResponse expectedResponse = new PaymentResponse("payment-id", 1L, internalId, PaymentStatus.PENDING, saved.getTimestamp(), BigDecimal.TEN);
        given(paymentMapper.toResponse(saved)).willReturn(expectedResponse);

        // action
        PaymentResponse actual = service.createPayment(request);

        // assertThat
        assertThat(actual).isEqualTo(expectedResponse);
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }


    @Test
    void createPayment_userMismatch_throwsAccessDenied() {
        // given
        given(authenticationService.getInternalId()).willReturn(100L);
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 42L, BigDecimal.ONE);

        // action & assertThat
        assertThatThrownBy(() -> service.createPayment(request))
                .isInstanceOf(UserAccessDeniedException.class);
        verifyNoInteractions(paymentRepository);
    }


    @Test
    void updatePaymentStatus_validRequest_updatesAndReturnsPaymentResponse() {
        // given
        String paymentId = "p1";
        long internalId = 7L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        Payment existing = Payment.builder()
                .id(paymentId)
                .userId(internalId)
                .status(PaymentStatus.PENDING)
                .build();
        // Replaced: when(paymentRepository.findByIdAndUserId(paymentId, internalId)).thenReturn(Optional.of(existing));
        given(paymentRepository.findByIdAndUserId(paymentId, internalId)).willReturn(Optional.of(existing));

        UpdatePaymentStatusRequest req = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);

        Payment updated = Payment.builder()
                .id(paymentId)
                .userId(internalId)
                .status(PaymentStatus.SUCCESS)
                .build();
        given(paymentRepository.save(existing)).willReturn(updated);

        PaymentResponse response = new PaymentResponse(paymentId, null, internalId, PaymentStatus.SUCCESS, Instant.now(), null);
        given(paymentMapper.toResponse(updated)).willReturn(response);

        // action
        PaymentResponse actual = service.updatePaymentStatus(paymentId, req);

        // assertThat
        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).findByIdAndUserId(paymentId, internalId);
        verify(paymentRepository).save(existing);
    }


    @Test
    void findPaymentsByUserId_userMismatch_throwsAccessDenied() {
        // given
        long internalId = 11L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        long requestedUser = 22L;

        // action & assertThat
        assertThatThrownBy(() -> service.findPaymentsByUserId(requestedUser))
                .isInstanceOf(UserAccessDeniedException.class);
        verifyNoInteractions(paymentRepository);
    }


    @Test
    void findPaymentTotalForPeriod_validRequest_returnsTotal() {
        // given
        long internalId = 5L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        Instant start = Instant.parse("2023-01-01T00:00:00Z");
        Instant end = Instant.parse("2023-01-31T23:59:59Z");
        BigDecimal total = BigDecimal.valueOf(1234.56);
        given(paymentRepository.findPaymentTotalForPeriodAndUserId(start, end, internalId)).willReturn(total);

        // action
        PaymentTotalResponse resp = service.findPaymentTotalForPeriod(start, end);

        // assertThat
        assertThat(resp.paymentTotal()).isEqualByComparingTo(total);
        verify(paymentRepository).findPaymentTotalForPeriodAndUserId(start, end, internalId);
    }


    @Test
    void deletePayment_ownerMatches_deletesPayment() {
        // given
        long internalId = 9L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        String id = "to-delete";
        Payment owned = Payment.builder().id(id).userId(internalId).build();
        given(paymentRepository.findById(id)).willReturn(Optional.of(owned));

        // action
        service.deletePayment(id);

        // assertThat
        verify(paymentRepository).delete(owned);
    }

    @Test
    void updatePaymentStatus_paymentNotFound_throwsException() {
        // given
        String paymentId = "non-existent-id";
        long internalId = 7L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        given(paymentRepository.findByIdAndUserId(paymentId, internalId))
                .willReturn(Optional.empty());

        UpdatePaymentStatusRequest req = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);

        // action & assertThat
        assertThatThrownBy(() -> service.updatePaymentStatus(paymentId, req))
                .isInstanceOf(PaymentNotFoundException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void deletePayment_paymentNotFound_doesNothing() {
        // given
        long internalId = 9L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        String id = "missing-id";
        given(paymentRepository.findById(id)).willReturn(Optional.empty());

        // action
        service.deletePayment(id);

        // assertThat
        verify(paymentRepository, never()).delete(any());
    }

    @Test
    void deletePayment_userMismatch_doesNotDelete() {
        // given
        long internalId = 9L;
        long otherUserId = 99L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        String id = "other-users-payment";
        Payment otherPayment = Payment.builder()
                .id(id)
                .userId(otherUserId) // Different user
                .build();

        given(paymentRepository.findById(id)).willReturn(Optional.of(otherPayment));

        // action
        service.deletePayment(id);

        // assertThat
        verify(paymentRepository, never()).delete(any());
    }

    @Test
    void findPaymentsByOrderId_validRequest_returnsList() {
        // given
        long internalId = 5L;
        long orderId = 101L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        Payment payment = Payment.builder().id("p1").orderId(orderId).userId(internalId).build();
        given(paymentRepository.findByOrderIdAndUserId(orderId, internalId))
                .willReturn(List.of(payment));

        PaymentResponse response = new PaymentResponse("p1", orderId, internalId, PaymentStatus.PENDING, Instant.now(), BigDecimal.TEN);
        given(paymentMapper.toResponse(payment)).willReturn(response);

        // action
        List<PaymentResponse> results = service.findPaymentsByOrderId(orderId);

        // assertThat
        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).isEqualTo(response);
    }

    @Test
    void findPaymentsByUserId_validRequest_returnsList() {
        // given
        long internalId = 33L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        Payment payment = Payment.builder().id("p1").userId(internalId).build();
        given(paymentRepository.findByUserId(internalId)).willReturn(List.of(payment));

        PaymentResponse response = new PaymentResponse("p1", 1L, internalId, PaymentStatus.PENDING, Instant.now(), BigDecimal.TEN);
        given(paymentMapper.toResponse(payment)).willReturn(response);

        // action
        List<PaymentResponse> results = service.findPaymentsByUserId(internalId);

        // assertThat
        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).isEqualTo(response);
    }

    @Test
    void findPaymentsByStatuses_validRequest_returnsList() {
        // given
        long internalId = 55L;
        given(authenticationService.getInternalId()).willReturn(internalId);

        List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING, PaymentStatus.FAILED);

        Payment payment = Payment.builder().id("p1").status(PaymentStatus.PENDING).userId(internalId).build();
        given(paymentRepository.findByStatusInAndUserId(statuses, internalId))
                .willReturn(List.of(payment));

        PaymentResponse response = new PaymentResponse("p1", 1L, internalId, PaymentStatus.PENDING, Instant.now(), BigDecimal.TEN);
        given(paymentMapper.toResponse(payment)).willReturn(response);

        // action
        List<PaymentResponse> results = service.findPaymentsByStatuses(statuses);

        // assertThat
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo(PaymentStatus.PENDING);
    }
}