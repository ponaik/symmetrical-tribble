package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.service.AuthenticationService;
import com.intern.paymentservice.service.PaymentFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class PaymentConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentFacade paymentFacade;
    private final AuthenticationService authenticationService;

    public PaymentConsumer(
            ObjectMapper objectMapper,
            PaymentFacade paymentFacade, AuthenticationService authenticationService) {

        this.objectMapper = objectMapper;
        this.paymentFacade = paymentFacade;
        this.authenticationService = authenticationService;
    }

    @KafkaListener(topics = "CREATE_ORDER", groupId = "payment-service")
    public void consumeCreatePayment(String message) {
        try {
            CreatePaymentRequest request = objectMapper.readValue(message, CreatePaymentRequest.class);
            log.debug("Payment Service received CREATE_PAYMENT event: {}", request);

            authenticationService.setBrokerAuthenticationInContext();
            paymentFacade.createPayment(request);
        } catch (JacksonException e) {
            log.error("Failed to parse message into CreatePaymentRequest: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Critical error during payment processing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process message for retry/DLT", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
