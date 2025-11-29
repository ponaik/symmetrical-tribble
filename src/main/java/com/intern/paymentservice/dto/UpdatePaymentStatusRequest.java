package com.intern.paymentservice.dto;

import com.intern.paymentservice.model.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(
        @NotNull(message = "Payment status must be provided")
        PaymentStatus status
) {}
