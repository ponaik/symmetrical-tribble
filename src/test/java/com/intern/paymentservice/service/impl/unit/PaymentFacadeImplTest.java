package com.intern.paymentservice.service.impl.unit;

import com.intern.paymentservice.client.PaymentResultClient;
import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.service.PaymentService;
import com.intern.paymentservice.service.broker.PaymentProducer;
import com.intern.paymentservice.service.impl.PaymentFacadeImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentFacadeImplTest {

    @Mock
    PaymentService paymentService;

    @Mock
    PaymentProducer paymentProducer;

    @Mock
    PaymentResultClient paymentResultClient;

    @InjectMocks
    PaymentFacadeImpl facade;

    @Test
    void createPayment_simulationSuccess_updatesStatusAndSendsEvents() {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 100L, BigDecimal.TEN);

        // 1. Initial creation (Pending)
        PaymentResponse pendingResponse = new PaymentResponse("p1", 1L, 100L, PaymentStatus.PENDING, Instant.now(), BigDecimal.TEN);
        given(paymentService.createPayment(request)).willReturn(pendingResponse);

        // 2. Simulation (Even number = Success)
        given(paymentResultClient.getPaymentResult()).willReturn(200);

        // 3. Update (Success)
        PaymentResponse successResponse = new PaymentResponse("p1", 1L, 100L, PaymentStatus.SUCCESS, Instant.now(), BigDecimal.TEN);
        given(paymentService.updatePaymentStatus(eq("p1"), any(UpdatePaymentStatusRequest.class)))
                .willReturn(successResponse);

        // action
        PaymentResponse actual = facade.createPayment(request);

        // assertThat
        assertThat(actual).isEqualTo(pendingResponse);

        verify(paymentService).createPayment(request);
        verify(paymentResultClient).getPaymentResult();

        // 3. Service update called with SUCCESS
        ArgumentCaptor<UpdatePaymentStatusRequest> updateCaptor = ArgumentCaptor.forClass(UpdatePaymentStatusRequest.class);
        verify(paymentService).updatePaymentStatus(eq("p1"), updateCaptor.capture());
        assertThat(updateCaptor.getValue().status()).isEqualTo(PaymentStatus.SUCCESS);

        // 4. Producer called TWICE (Once for create PENDING, once for update SUCCESS)
        ArgumentCaptor<PaymentResponse> producerCaptor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentProducer, times(2)).sendPaymentUpdate(producerCaptor.capture());

        List<PaymentResponse> capturedEvents = producerCaptor.getAllValues();
        assertThat(capturedEvents.get(0).status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedEvents.get(1).status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void createPayment_simulationFailure_updatesStatusAndSendsEvents() {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 100L, BigDecimal.TEN);

        PaymentResponse pendingResponse = new PaymentResponse("p1", 1L, 100L, PaymentStatus.PENDING, Instant.now(), BigDecimal.TEN);
        given(paymentService.createPayment(request)).willReturn(pendingResponse);

        // Simulation (Odd number = Failed)
        given(paymentResultClient.getPaymentResult()).willReturn(201);

        PaymentResponse failedResponse = new PaymentResponse("p1", 1L, 100L, PaymentStatus.FAILED, Instant.now(), BigDecimal.TEN);
        given(paymentService.updatePaymentStatus(eq("p1"), any(UpdatePaymentStatusRequest.class)))
                .willReturn(failedResponse);

        // action
        facade.createPayment(request);

        // assertThat
        // Verify Service update called with FAILED
        ArgumentCaptor<UpdatePaymentStatusRequest> updateCaptor = ArgumentCaptor.forClass(UpdatePaymentStatusRequest.class);
        verify(paymentService).updatePaymentStatus(eq("p1"), updateCaptor.capture());
        assertThat(updateCaptor.getValue().status()).isEqualTo(PaymentStatus.FAILED);

        // Verify Producer called with FAILED as the second event
        ArgumentCaptor<PaymentResponse> producerCaptor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentProducer, times(2)).sendPaymentUpdate(producerCaptor.capture());
        assertThat(producerCaptor.getAllValues().get(1).status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void updatePaymentStatus_validRequest_callsServiceAndProducer() {
        // given
        String id = "p1";
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);
        PaymentResponse response = new PaymentResponse(id, 1L, 100L, PaymentStatus.SUCCESS, Instant.now(), BigDecimal.TEN);

        given(paymentService.updatePaymentStatus(id, request)).willReturn(response);

        // action
        PaymentResponse actual = facade.updatePaymentStatus(id, request);

        // assertThat
        assertThat(actual).isEqualTo(response);
        verify(paymentService).updatePaymentStatus(id, request);
        verify(paymentProducer).sendPaymentUpdate(response);
    }

    @Test
    void deletePayment_delegatesToService() {
        // given
        String id = "del-1";

        // action
        facade.deletePayment(id);

        // assertThat
        verify(paymentService).deletePayment(id);
    }

    @Test
    void findPaymentsByOrderId_delegatesToService() {
        // given
        Long orderId = 123L;
        PaymentResponse response = new PaymentResponse("p1", orderId, 1L, PaymentStatus.SUCCESS, Instant.now(), BigDecimal.TEN);
        given(paymentService.findPaymentsByOrderId(orderId)).willReturn(List.of(response));

        // action
        List<PaymentResponse> actual = facade.findPaymentsByOrderId(orderId);

        // assertThat
        assertThat(actual).containsExactly(response);
        verify(paymentService).findPaymentsByOrderId(orderId);
    }

    @Test
    void findPaymentsByUserId_delegatesToService() {
        // given
        Long userId = 456L;
        PaymentResponse response = new PaymentResponse("p1", 1L, userId, PaymentStatus.SUCCESS, Instant.now(), BigDecimal.TEN);
        given(paymentService.findPaymentsByUserId(userId)).willReturn(List.of(response));

        // action
        List<PaymentResponse> actual = facade.findPaymentsByUserId(userId);

        // assertThat
        assertThat(actual).containsExactly(response);
        verify(paymentService).findPaymentsByUserId(userId);
    }

    @Test
    void findPaymentsByStatuses_delegatesToService() {
        // given
        List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING);
        PaymentResponse response = new PaymentResponse("p1", 1L, 1L, PaymentStatus.PENDING, Instant.now(), BigDecimal.TEN);
        given(paymentService.findPaymentsByStatuses(statuses)).willReturn(List.of(response));

        // action
        List<PaymentResponse> actual = facade.findPaymentsByStatuses(statuses);

        // assertThat
        assertThat(actual).containsExactly(response);
        verify(paymentService).findPaymentsByStatuses(statuses);
    }

    @Test
    void findPaymentTotalForPeriod_delegatesToService() {
        // given
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        PaymentTotalResponse total = new PaymentTotalResponse(BigDecimal.valueOf(100));
        given(paymentService.findPaymentTotalForPeriod(start, end)).willReturn(total);

        // action
        PaymentTotalResponse actual = facade.findPaymentTotalForPeriod(start, end);

        // assertThat
        assertThat(actual).isEqualTo(total);
        verify(paymentService).findPaymentTotalForPeriod(start, end);
    }
}