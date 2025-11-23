package com.intern.paymentservice.dto;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        Long orderId,
        Long userId,
        BigDecimal paymentAmount
) {}
