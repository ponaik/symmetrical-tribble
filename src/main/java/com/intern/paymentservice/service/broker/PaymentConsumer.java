package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.aspect.annotation.BrokerAuthentication;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.service.PaymentFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentFacade paymentFacade;

    @BrokerAuthentication
    @KafkaListener(topics = "CREATE_ORDER", groupId = "payment-service")
    public void consumeCreatePayment(@Valid CreatePaymentRequest request) {
        log.debug("Payment Service received CREATE_PAYMENT event: {}", request);
        paymentFacade.createPayment(request);
    }
}