package com.intern.paymentservice.controller;


import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.enums.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/test")
    public ResponseEntity<Payment> test() {

        Instant now = Instant.ofEpochSecond(1763840570L);

        Payment payment = Payment.builder()
                .orderId(12L)
                .userId(12L)
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .paymentAmount(BigDecimal.valueOf(1.11))
                .build();




        BigDecimal paymentTotalForPeriod = paymentRepository.findPaymentTotalForPeriod(now, Instant.now());


        return null;
    }
}
