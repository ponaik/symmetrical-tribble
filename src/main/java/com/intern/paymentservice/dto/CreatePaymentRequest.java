package com.intern.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull
        @Positive
        Long orderId,

        @Positive
        Long userId,

        @NotNull
        @PositiveOrZero
        BigDecimal paymentAmount
) implements Serializable {}
