package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@NullMarked
public class PaymentProducer {

    private final KafkaTemplate<String, PaymentResponse> kafkaTemplate;

    @Autowired
    public PaymentProducer(KafkaTemplate<String, PaymentResponse> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentUpdate(PaymentResponse response) {
        kafkaTemplate.send("UPDATE_PAYMENT", response.orderId().toString(), response);
        log.debug("Sent UPDATE_PAYMENT event: {}", response);
    }
}
