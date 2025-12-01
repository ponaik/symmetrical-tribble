package com.intern.paymentservice.integration.service.broker;

import com.intern.paymentservice.TestcontainersConfiguration;
import com.intern.paymentservice.client.PaymentResultClient;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.integration.NoSecurityConfig;
import com.intern.paymentservice.model.Payment;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@Tag("integration")
@Import({TestcontainersConfiguration.class, NoSecurityConfig.class})
@ActiveProfiles("test")
@SpringBootTest
@NullMarked
class KafkaSimulationFlowIntegrationTest {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    PaymentRepository paymentRepository;

    @MockitoBean
    PaymentResultClient paymentResultClient;

    @Test
    void givenCreateOrderEvent_whenResultIsEven_thenPaymentFinalStatusIsSuccess() {
        // arrange
        paymentRepository.deleteAll();
        Long orderId = 200L;
        CreatePaymentRequest request = new CreatePaymentRequest(orderId, 54321L, BigDecimal.valueOf(50.00));

        // Mock behavior: Return EVEN number (e.g., 200) -> Should trigger PaymentStatus.SUCCESS
        when(paymentResultClient.getPaymentResult()).thenReturn(200);

        // act
        kafkaTemplate.send("CREATE_ORDER", orderId.toString(), request);

        // assert
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<Payment> payments = paymentRepository.findByOrderId(orderId);
                    assertThat(payments).isNotEmpty();

                    // We assert that the simulation logic ran and updated the DB
                    assertThat(payments.get(0).getStatus())
                            .as("Status should be updated to SUCCESS based on even simulation result")
                            .isEqualTo(PaymentStatus.SUCCESS);
                });
    }

}
