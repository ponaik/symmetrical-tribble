package com.intern.paymentservice.repository;

import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@NullMarked
public interface PaymentRepository
        extends MongoRepository<Payment, String>,
        PaymentAggregationRepository {

    Optional<Payment> findByIdAndUserId(String id, Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByOrderIdAndUserId(Long orderId, Long userId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    List<Payment> findByStatusInAndUserId(List<PaymentStatus> statuses, Long userId);
}
