package com.intern.paymentservice.dto;

import com.intern.paymentservice.model.PaymentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        Long orderId,
        Long userId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount
)  implements Serializable {}
