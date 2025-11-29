package com.intern.paymentservice.service.broker;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.service.PaymentFacade;
import com.intern.paymentservice.service.security.TokenRetrievalService;
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
    private final TokenRetrievalService tokenRetrievalService;

    public PaymentConsumer(
            ObjectMapper objectMapper,
            PaymentFacade paymentFacade,
            TokenRetrievalService tokenRetrievalService) {

        this.objectMapper = objectMapper;
        this.paymentFacade = paymentFacade;
        this.tokenRetrievalService = tokenRetrievalService;
    }

    @KafkaListener(topics = "CREATE_ORDER", groupId = "payment-service")
    public void consumeCreatePayment(String message) {
        try {
            CreatePaymentRequest request = objectMapper.readValue(message, CreatePaymentRequest.class);
            log.debug("Payment Service received CREATE_PAYMENT event: {}", request);

            tokenRetrievalService.setAuthenticationInContext();
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
