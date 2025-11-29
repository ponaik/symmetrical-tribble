package com.intern.paymentservice.service.impl;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.service.AuthenticationService;
import com.intern.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@Service
@Primary
public class PaymentAuthorizationDecorator implements PaymentService {

    private final AuthenticationService authenticationService;
    private final PaymentService paymentServiceImpl; // Admin implementation
    private final PaymentService userPaymentServiceImpl; // User implementation

    @Autowired
    public PaymentAuthorizationDecorator(AuthenticationService authenticationService, PaymentService paymentServiceImpl, PaymentService userPaymentServiceImpl) {
        this.authenticationService = authenticationService;
        this.paymentServiceImpl = paymentServiceImpl;
        this.userPaymentServiceImpl = userPaymentServiceImpl;
    }

    private PaymentService getDelegate() {
        if (authenticationService.isAdmin()) {
            log.debug("User is an admin. Routing to PaymentServiceImpl.");
            return paymentServiceImpl;
        } else if (authenticationService.isUser()) {
            log.debug("User is a standard user. Routing to UserPaymentServiceImpl.");
            return userPaymentServiceImpl;
        } else {
            log.warn("Unauthorized access attempt by a user lacking 'admin' or 'user' role.");
            throw new ResponseStatusException(FORBIDDEN, "Access Denied: Insufficient authority for payment service operations.");
        }
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        return getDelegate().createPayment(request);
    }

    @Override
    public PaymentResponse updatePaymentStatus(String id, UpdatePaymentStatusRequest request) {
        return getDelegate().updatePaymentStatus(id, request);
    }

    @Override
    public void deletePayment(String id) {
        getDelegate().deletePayment(id);
    }

    @Override
    public List<PaymentResponse> findPaymentsByOrderId(Long orderId) {
        return getDelegate().findPaymentsByOrderId(orderId);
    }

    @Override
    public List<PaymentResponse> findPaymentsByUserId(Long userId) {
        return getDelegate().findPaymentsByUserId(userId);
    }

    @Override
    public List<PaymentResponse> findPaymentsByStatuses(List<PaymentStatus> statuses) {
        return getDelegate().findPaymentsByStatuses(statuses);
    }

    @Override
    public PaymentTotalResponse findPaymentTotalForPeriod(Instant start, Instant end) {
        return getDelegate().findPaymentTotalForPeriod(start, end);
    }
}