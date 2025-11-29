package com.intern.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "Order ID must be provided")
        @Positive(message = "Order ID must be a positive number")
        Long orderId,

        @Positive(message = "User ID must be a positive number")
        Long userId,

        @NotNull(message = "Payment amount must be provided")
        @PositiveOrZero(message = "Payment amount must be zero or a positive number")
        BigDecimal paymentAmount
) implements Serializable {}
