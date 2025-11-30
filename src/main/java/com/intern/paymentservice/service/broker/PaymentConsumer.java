package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.service.AuthenticationService;
import com.intern.paymentservice.service.PaymentFacade;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
public class PaymentConsumer {

    private final PaymentFacade paymentFacade;
    private final AuthenticationService authenticationService;

    @Autowired
    public PaymentConsumer(PaymentFacade paymentFacade, AuthenticationService authenticationService) {
        this.paymentFacade = paymentFacade;
        this.authenticationService = authenticationService;
    }

    @KafkaListener(topics = "CREATE_ORDER", groupId = "payment-service")
    public void consumeCreatePayment(@Valid CreatePaymentRequest request) {
        try {
            log.debug("Payment Service received CREATE_PAYMENT event: {}", request);

            authenticationService.setBrokerAuthenticationInContext();
            paymentFacade.createPayment(request);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
