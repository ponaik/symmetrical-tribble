package com.intern.paymentservice.service;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request);

    void deletePayment(String id);

    List<PaymentResponse> findPaymentsByOrderId(Long orderId);

    List<PaymentResponse> findPaymentsByUserId(Long userId);

    List<PaymentResponse> findPaymentsByStatuses(List<PaymentStatus> statuses);

    BigDecimal findPaymentTotalForPeriod(Instant start, Instant end);
}
