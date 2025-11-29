package com.intern.paymentservice.service.impl;

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
import com.intern.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class UserPaymentServiceImpl implements PaymentService {

    private final AuthenticationService authenticationService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Autowired
    public UserPaymentServiceImpl(AuthenticationService authenticationService, PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.authenticationService = authenticationService;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        long internalId = authenticationService.getInternalId();
        if (!request.userId().equals(internalId)) {
            throw new UserAccessDeniedException(internalId);
        }

        Payment payment = paymentMapper.toEntity(request);
        payment.setTimestamp(Instant.now());
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.debug("Persisted Payment object for Payment with id {} by userId {}", saved.getId(), internalId);
        return paymentMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public PaymentResponse updatePaymentStatus(String id, UpdatePaymentStatusRequest request) {
        long internalId = authenticationService.getInternalId();
        Payment payment = paymentRepository.findByIdAndUserId(id, internalId)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        payment.setStatus(request.status());

        Payment updated = paymentRepository.save(payment);
        log.debug("Updated Payment status to {} for Payment with id {} by userId {}", request.status(), id, internalId);
        return paymentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deletePayment(String id) {
        long internalId = authenticationService.getInternalId();
        paymentRepository.findById(id).ifPresent(payment -> {
            if (payment.getUserId().equals(internalId)) {
                paymentRepository.delete(payment);
            }
        });

        log.debug("Deleted Payment with id {} by userId {}", id, internalId);
    }

    @Override
    public List<PaymentResponse> findPaymentsByOrderId(Long orderId) {
        long internalId = authenticationService.getInternalId();
        return paymentRepository.findByOrderIdAndUserId(orderId, internalId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> findPaymentsByUserId(Long userId) {
        long internalId = authenticationService.getInternalId();
        if (!userId.equals(internalId)) {
            throw new UserAccessDeniedException(internalId);
        }

        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> findPaymentsByStatuses(List<PaymentStatus> statuses) {
        long internalId = authenticationService.getInternalId();
        return paymentRepository.findByStatusInAndUserId(statuses, internalId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public PaymentTotalResponse findPaymentTotalForPeriod(Instant start, Instant end) {
        long internalId = authenticationService.getInternalId();
        return new PaymentTotalResponse(paymentRepository.findPaymentTotalForPeriodAndUserId(start, end, internalId));
    }
}
