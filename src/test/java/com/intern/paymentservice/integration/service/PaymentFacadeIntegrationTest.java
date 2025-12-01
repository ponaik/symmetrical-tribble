package com.intern.paymentservice.integration.service;

import com.intern.paymentservice.TestcontainersConfiguration;
import com.intern.paymentservice.client.PaymentResultClient;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.integration.NoSecurityConfig;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.repository.PaymentRepository;
import com.intern.paymentservice.service.PaymentFacade;
import com.intern.paymentservice.service.impl.PaymentServiceImpl;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.intern.paymentservice.service.AuthenticationService.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Import({TestcontainersConfiguration.class, NoSecurityConfig.class})
@ActiveProfiles("test")
@SpringBootTest
@Tag("integration")
class PaymentFacadeIntegrationTest {

    @Autowired
    PaymentFacade facade;

    @Autowired
    KafkaContainer kafkaContainer;

    @Autowired
    PaymentServiceImpl paymentServiceImpl;

    @MockitoBean
    PaymentResultClient paymentResultClient;

    @Autowired
    private PaymentRepository paymentRepository;

    KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        // Create a fresh consumer for each test with a unique group id
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("UPDATE_PAYMENT"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) consumer.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenPaymentCreated_whenPaymentSucceeds_thenSuccessEventsEmitted() {
        // given
        String orderId = "10";
        CreatePaymentRequest request = new CreatePaymentRequest(Long.valueOf(orderId), 20L, BigDecimal.TEN);
        given(paymentResultClient.getPaymentResult()).willReturn(200);

        // when
        facade.createPayment(request);

        // then
        List<ConsumerRecord<String, String>> records = getRecordsForKey(orderId, 2);

        assertThat(records).hasSizeGreaterThanOrEqualTo(2);

        boolean sawPending = false;
        boolean sawSuccess = false;

        for (ConsumerRecord<String, String> r : records) {
            String v = r.value();
            if (v.contains("\"status\":\"PENDING\"")) sawPending = true;
            if (v.contains("\"status\":\"SUCCESS\"")) sawSuccess = true;
        }

        assertThat(sawPending).as("Should have seen PENDING status").isTrue();
        assertThat(sawSuccess).as("Should have seen SUCCESS status").isTrue();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {ADMIN})
    void givenExistingPayment_whenStatusIsUpdated_thenSuccessEventEmitted() {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest(100L, 200L, BigDecimal.ONE);
        PaymentResponse created = paymentServiceImpl.createPayment(request);

        // when
        UpdatePaymentStatusRequest update = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);
        facade.updatePaymentStatus(created.id(), update);

        // then
        List<ConsumerRecord<String, String>> records = getRecordsForKey(created.orderId().toString(), 1);

        assertThat(records).isNotEmpty();

        boolean sawSuccess = false;
        for (ConsumerRecord<String, String> r : records) {
            if (r.value().contains("\"status\":\"SUCCESS\"")) {
                sawSuccess = true;
            }
        }

        assertThat(sawSuccess).as("Should have seen SUCCESS status").isTrue();
    }

    // --- Helpers ---
    private List<ConsumerRecord<String, String>> getRecordsForKey(String targetKey, int expectedCount) {
        List<ConsumerRecord<String, String>> relevantRecords = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10_000; // 10 seconds timeout

        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, String> record : records) {
                // FILTER: Only keep records that belong to THIS test
                if (record.key() != null && record.key().equals(targetKey)) {
                    relevantRecords.add(record);
                }
            }

            if (relevantRecords.size() >= expectedCount) {
                return relevantRecords;
            }
        }

        return relevantRecords;
    }
}

