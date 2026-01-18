package com.intern.paymentservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PaymentResultClient {

    private final WebClient webClient;

    public PaymentResultClient(
            @Value("${server.port}") String port,
            WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:" + port).build();
    }

    public Integer getPaymentResult() {
        return webClient.get()
                .uri("/totallyLegitDecisionApi")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Integer.class)
                .block();
    }
}
