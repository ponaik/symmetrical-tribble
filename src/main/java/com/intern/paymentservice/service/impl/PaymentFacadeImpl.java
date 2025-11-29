package com.intern.paymentservice.service.impl;

import com.intern.paymentservice.client.DecisionClient;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.service.PaymentFacade;
import com.intern.paymentservice.service.PaymentService;
import com.intern.paymentservice.service.broker.PaymentProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class PaymentFacadeImpl implements PaymentFacade {

    private final PaymentService paymentService;
    private final PaymentProducer paymentProducer;
    private final DecisionClient decisionClient;

    @Autowired
    public PaymentFacadeImpl(PaymentService paymentService, PaymentProducer paymentProducer, DecisionClient decisionClient) {
        this.paymentService = paymentService;
        this.paymentProducer = paymentProducer;
        this.decisionClient = decisionClient;
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        paymentProducer.sendPaymentResponse(response);

        // Simulate payment
        Integer paymentDecision = decisionClient.getPaymentDecision();
        PaymentStatus newStatus = (paymentDecision % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        updatePaymentStatus(response.id(), new UpdatePaymentStatusRequest(newStatus));

        return response;
    }

    @Override
    public PaymentResponse updatePaymentStatus(String id, UpdatePaymentStatusRequest request) {
        PaymentResponse response = paymentService.updatePaymentStatus(id, request);
        paymentProducer.sendPaymentUpdate(response);

        return response;
    }

    @Override
    public void deletePayment(String id) {
        paymentService.deletePayment(id);
    }

    @Override
    public List<PaymentResponse> findPaymentsByOrderId(Long orderId) {
        return paymentService.findPaymentsByOrderId(orderId);
    }

    @Override
    public List<PaymentResponse> findPaymentsByUserId(Long userId) {
        return paymentService.findPaymentsByUserId(userId);
    }

    @Override
    public List<PaymentResponse> findPaymentsByStatuses(List<PaymentStatus> statuses) {
        return paymentService.findPaymentsByStatuses(statuses);
    }

    @Override
    public PaymentTotalResponse findPaymentTotalForPeriod(Instant start, Instant end) {
        return paymentService.findPaymentTotalForPeriod(start, end);
    }
}