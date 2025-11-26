package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class PaymentConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final PaymentProducer paymentProducer;

    public PaymentConsumer(ObjectMapper objectMapper, PaymentService paymentService, PaymentProducer paymentProducer) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.paymentProducer = paymentProducer;
    }

    @KafkaListener(topics = "CREATE_ORDER", groupId = "payment-service")
    public void consumeCreatePayment(String message) {
        CreatePaymentRequest request = objectMapper.readValue(message, CreatePaymentRequest.class);
        log.debug("Payment Service received CREATE_PAYMENT event: {}", request);

        PaymentResponse payment = paymentService.createPayment(request);

        paymentProducer.sendPaymentResponse(payment);
    }
}
