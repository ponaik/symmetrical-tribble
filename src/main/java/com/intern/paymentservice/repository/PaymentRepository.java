package com.intern.paymentservice.repository;

import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository
        extends MongoRepository<Payment, String>,
        PaymentAggregationRepository {

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

}
