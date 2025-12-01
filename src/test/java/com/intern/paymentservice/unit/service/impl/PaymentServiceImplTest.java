package com.intern.paymentservice.unit.service.impl;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.exception.PaymentNotFoundException;
import com.intern.paymentservice.mapper.PaymentMapper;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentServiceImplTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentMapper paymentMapper;

    @InjectMocks
    PaymentServiceImpl service;

    @Test
    void createPayment_validRequest_savesAndReturnsResponse() {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 100L, BigDecimal.TEN);
        
        Payment mappedEntity = Payment.builder()
                .orderId(1L)
                .userId(100L)
                .paymentAmount(BigDecimal.TEN)
                .build();
        given(paymentMapper.toEntity(request)).willReturn(mappedEntity);

        Payment savedEntity = Payment.builder()
                .id("p1")
                .orderId(1L)
                .userId(100L)
                .paymentAmount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();
        given(paymentRepository.save(any(Payment.class))).willReturn(savedEntity);

        PaymentResponse expectedResponse = new PaymentResponse("p1", 1L, 100L, PaymentStatus.PENDING, savedEntity.getTimestamp(), BigDecimal.TEN);
        given(paymentMapper.toResponse(savedEntity)).willReturn(expectedResponse);

        // action
        PaymentResponse actual = service.createPayment(request);

        // assertThat
        assertThat(actual).isEqualTo(expectedResponse);

        // Verify service logic (setting defaults)
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        
        Payment capturedPayment = captor.getValue();
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedPayment.getTimestamp()).isNotNull();
    }

    @Test
    void updatePaymentStatus_paymentExists_updatesStatusAndReturnsResponse() {
        // given
        String paymentId = "p1";
        Payment existingPayment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PENDING)
                .build();
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(existingPayment));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);

        Payment updatedPayment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.SUCCESS)
                .build();
        given(paymentRepository.save(existingPayment)).willReturn(updatedPayment);

        PaymentResponse expectedResponse = new PaymentResponse(paymentId, null, null, PaymentStatus.SUCCESS, null, null);
        given(paymentMapper.toResponse(updatedPayment)).willReturn(expectedResponse);

        // action
        PaymentResponse actual = service.updatePaymentStatus(paymentId, request);

        // assertThat
        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    void updatePaymentStatus_paymentNotFound_throwsException() {
        // given
        String paymentId = "missing-id";
        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);

        // action & assertThat
        assertThatThrownBy(() -> service.updatePaymentStatus(paymentId, request))
                .isInstanceOf(PaymentNotFoundException.class);
        
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void deletePayment_callsRepositoryDeleteById() {
        // given
        String paymentId = "p1";

        // action
        service.deletePayment(paymentId);

        // assertThat
        verify(paymentRepository).deleteById(paymentId);
    }

    @Test
    void findPaymentsByOrderId_found_returnsList() {
        // given
        Long orderId = 123L;
        Payment payment = Payment.builder().id("p1").orderId(orderId).build();
        given(paymentRepository.findByOrderId(orderId)).willReturn(List.of(payment));

        PaymentResponse response = new PaymentResponse("p1", orderId, null, null, null, null);
        given(paymentMapper.toResponse(payment)).willReturn(response);

        // action
        List<PaymentResponse> actual = service.findPaymentsByOrderId(orderId);

        // assertThat
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(response);
    }

    @Test
    void findPaymentsByOrderId_notFound_returnsEmptyList() {
        // given
        Long orderId = 999L;
        given(paymentRepository.findByOrderId(orderId)).willReturn(Collections.emptyList());

        // action
        List<PaymentResponse> actual = service.findPaymentsByOrderId(orderId);

        // assertThat
        assertThat(actual).isEmpty();
        verify(paymentMapper, never()).toResponse(any());
    }

    @Test
    void findPaymentsByUserId_found_returnsList() {
        // given
        Long userId = 456L;
        Payment payment = Payment.builder().id("p1").userId(userId).build();
        given(paymentRepository.findByUserId(userId)).willReturn(List.of(payment));

        PaymentResponse response = new PaymentResponse("p1", null, userId, null, null, null);
        given(paymentMapper.toResponse(payment)).willReturn(response);

        // action
        List<PaymentResponse> actual = service.findPaymentsByUserId(userId);

        // assertThat
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(response);
    }

    @Test
    void findPaymentsByStatuses_found_returnsList() {
        // given
        List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING, PaymentStatus.FAILED);
        Payment payment = Payment.builder().id("p1").status(PaymentStatus.PENDING).build();
        
        given(paymentRepository.findByStatusIn(statuses)).willReturn(List.of(payment));

        PaymentResponse response = new PaymentResponse("p1", null, null, PaymentStatus.PENDING, null, null);
        given(paymentMapper.toResponse(payment)).willReturn(response);

        // action
        List<PaymentResponse> actual = service.findPaymentsByStatuses(statuses);

        // assertThat
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void findPaymentTotalForPeriod_returnsCalculatedTotal() {
        // given
        Instant start = Instant.parse("2023-01-01T00:00:00Z");
        Instant end = Instant.parse("2023-01-31T23:59:59Z");
        BigDecimal total = new BigDecimal("500.00");

        given(paymentRepository.findPaymentTotalForPeriod(start, end)).willReturn(total);

        // action
        PaymentTotalResponse actual = service.findPaymentTotalForPeriod(start, end);

        // assertThat
        assertThat(actual.paymentTotal()).isEqualTo(total);
        verify(paymentRepository).findPaymentTotalForPeriod(start, end);
    }
}