package com.intern.paymentservice.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentAggregationRepository {
    BigDecimal findPaymentTotalForPeriod(Instant start, Instant end);

    BigDecimal findPaymentTotalForPeriodAndUserId(Instant start, Instant end, Long userId);
}
