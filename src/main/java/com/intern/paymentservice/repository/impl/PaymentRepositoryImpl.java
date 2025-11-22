package com.intern.paymentservice.repository.impl;

import com.intern.paymentservice.repository.PaymentAggregationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

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
                    org.springframework.data.mongodb.core.query.Criteria.where("timestamp")
                        .gte(start).lte(end)
                ),
                group().sum("paymentAmount").as("totalAmount")
        );

        AggregationResults<TotalResult> results = mongoTemplate.aggregate(
                aggregation, "payments", TotalResult.class);

        TotalResult total = results.getUniqueMappedResult();
        return total != null ? total.getTotalAmount() : BigDecimal.ZERO;
    }

    public static class TotalResult {
        private BigDecimal totalAmount;
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }
}
