package com.intern.paymentservice.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record PaymentTotalResponse(
        BigDecimal paymentTotal
) implements Serializable {}
