package com.intern.paymentservice.service.impl;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.exception.PaymentNotFoundException;
import com.intern.paymentservice.mapper.PaymentMapper;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
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
        log.debug("Persisted Payment object for Payment with id {}", saved.getId());
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(String id, UpdatePaymentStatusRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        payment.setStatus(request.status());

        Payment updated = paymentRepository.save(payment);
        log.debug("Updated Payment status for Payment with id {} to {}", id, request.status());
        return paymentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
        log.debug("Deleted Payment with id {}", id);
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
    public List<PaymentResponse> findPaymentsByStatuses(List<PaymentStatus> statuses) {
        return paymentRepository.findByStatusIn(statuses)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentTotalResponse findPaymentTotalForPeriod(Instant start, Instant end) {
        return new PaymentTotalResponse(paymentRepository.findPaymentTotalForPeriod(start, end));
    }
}
