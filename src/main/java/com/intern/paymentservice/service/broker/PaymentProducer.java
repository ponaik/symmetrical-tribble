package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class PaymentProducer {

    private final KafkaTemplate<@NonNull String, @NonNull String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentProducer(KafkaTemplate<@NonNull String, @NonNull String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendPaymentResponse(PaymentResponse response) {
        String payload = objectMapper.writeValueAsString(response);
        kafkaTemplate.send("CREATE_PAYMENT", response.orderId().toString(), payload);
        log.debug("Sent CREATE_PAYMENT event: {}", payload);
    }
}
