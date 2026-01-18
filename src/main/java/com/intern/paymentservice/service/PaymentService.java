package com.intern.paymentservice.service;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.model.PaymentStatus;

import java.time.Instant;
import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse updatePaymentStatus(String id, UpdatePaymentStatusRequest request);

    void deletePayment(String id);

    List<PaymentResponse> findPaymentsByOrderId(Long orderId);

    List<PaymentResponse> findPaymentsByUserId(Long userId);

    List<PaymentResponse> findPaymentsByStatuses(List<PaymentStatus> statuses);

    PaymentTotalResponse findPaymentTotalForPeriod(Instant start, Instant end);
}
