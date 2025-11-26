package com.intern.paymentservice.service.impl;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.mapper.PaymentMapper;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = paymentMapper.toEntity(request);

        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(Instant.now());

        Payment saved = paymentRepository.save(payment);
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
    }

    @Override
    public List<PaymentResponse> findPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponse> findPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponse> findPaymentsByStatuses(List<String> statuses) {
        List<PaymentStatus> statusEnums = statuses.stream()
                .map(PaymentStatus::valueOf)
                .collect(Collectors.toList());

        return paymentRepository.findByStatusIn(statusEnums)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal findPaymentTotalForPeriod(Instant start, Instant end) {
        return paymentRepository.findPaymentTotalForPeriod(start, end);
    }
}
