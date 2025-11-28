package com.intern.paymentservice.repository.impl;

import com.intern.paymentservice.repository.PaymentAggregationRepository;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class PaymentRepositoryImpl implements PaymentAggregationRepository {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public PaymentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public BigDecimal findPaymentTotalForPeriod(Instant start, Instant end) {
        Aggregation aggregation = newAggregation(
                match(
                    where("timestamp")
                        .gte(start).lte(end)
                ),
                group().sum("paymentAmount").as("totalAmount")
        );

        AggregationResults<@NonNull TotalResult> results = mongoTemplate.aggregate(
                aggregation, "payments", TotalResult.class);

        TotalResult total = results.getUniqueMappedResult();
        return total != null ? total.getTotalAmount() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal findPaymentTotalForPeriodAndUserId(Instant start, Instant end, Long userId) {
        Aggregation aggregation = newAggregation(
                match(
                        where("timestamp")
                                .gte(start).lte(end)
                                .and("userId").is(userId)
                ),
                group().sum("paymentAmount").as("totalAmount")
        );

        AggregationResults<@NonNull TotalResult> results = mongoTemplate.aggregate(
                aggregation, "payments", TotalResult.class);

        TotalResult total = results.getUniqueMappedResult();
        return total != null ? total.getTotalAmount() : BigDecimal.ZERO;
    }

    @Getter
    public static class TotalResult {
        private BigDecimal totalAmount;
    }
}
