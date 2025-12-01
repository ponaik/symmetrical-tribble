package com.intern.paymentservice.unit.service.impl;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.service.AuthenticationService;
import com.intern.paymentservice.service.PaymentService;
import com.intern.paymentservice.service.impl.PaymentAuthorizationDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentAuthorizationDecoratorTest {

    @Mock
    AuthenticationService authenticationService;

    @Mock
    PaymentService adminPaymentService;

    @Mock
    PaymentService userPaymentService;

    PaymentAuthorizationDecorator decorator;

    @BeforeEach
    void setUp() {
        // explicit injection to ensure specific mocks are assigned to specific fields
        decorator = new PaymentAuthorizationDecorator(
                authenticationService,
                adminPaymentService,
                userPaymentService
        );
    }

    @Test
    void createPayment_isAdmin_delegatesToAdminService() {
        // given
        given(authenticationService.isAdmin()).willReturn(true);

        CreatePaymentRequest request = new CreatePaymentRequest(1L, 100L, BigDecimal.TEN);
        PaymentResponse expectedResponse = mock(PaymentResponse.class);
        given(adminPaymentService.createPayment(request)).willReturn(expectedResponse);

        // action
        PaymentResponse actual = decorator.createPayment(request);

        // assertThat
        assertThat(actual).isEqualTo(expectedResponse);
        then(adminPaymentService).should().createPayment(request);
        then(userPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void createPayment_isUser_delegatesToUserService() {
        // given
        given(authenticationService.isAdmin()).willReturn(false);
        given(authenticationService.isUser()).willReturn(true);

        CreatePaymentRequest request = new CreatePaymentRequest(1L, 100L, BigDecimal.TEN);
        PaymentResponse expectedResponse = mock(PaymentResponse.class);
        given(userPaymentService.createPayment(request)).willReturn(expectedResponse);

        // action
        PaymentResponse actual = decorator.createPayment(request);

        // assertThat
        assertThat(actual).isEqualTo(expectedResponse);
        then(userPaymentService).should().createPayment(request);
        then(adminPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void updatePaymentStatus_noRole_throwsForbiddenException() {
        // given
        given(authenticationService.isAdmin()).willReturn(false);
        given(authenticationService.isUser()).willReturn(false);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.SUCCESS);

        // action & assertThat
        assertThatThrownBy(() -> decorator.updatePaymentStatus("p1", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(FORBIDDEN);

        then(adminPaymentService).shouldHaveNoInteractions();
        then(userPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void deletePayment_isAdmin_delegatesToAdminService() {
        // given
        given(authenticationService.isAdmin()).willReturn(true);
        String id = "del-1";

        // action
        decorator.deletePayment(id);

        // assertThat
        then(adminPaymentService).should().deletePayment(id);
        then(userPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void findPaymentsByOrderId_isUser_delegatesToUserService() {
        // given
        given(authenticationService.isAdmin()).willReturn(false);
        given(authenticationService.isUser()).willReturn(true);
        Long orderId = 55L;

        // action
        decorator.findPaymentsByOrderId(orderId);

        // assertThat
        then(userPaymentService).should().findPaymentsByOrderId(orderId);
        then(adminPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void findPaymentsByUserId_isAdmin_delegatesToAdminService() {
        // given
        given(authenticationService.isAdmin()).willReturn(true);
        Long userId = 77L;

        // action
        decorator.findPaymentsByUserId(userId);

        // assertThat
        then(adminPaymentService).should().findPaymentsByUserId(userId);
        then(userPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void findPaymentsByStatuses_isUser_delegatesToUserService() {
        // given
        given(authenticationService.isAdmin()).willReturn(false);
        given(authenticationService.isUser()).willReturn(true);
        List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING);

        // action
        decorator.findPaymentsByStatuses(statuses);

        // assertThat
        then(userPaymentService).should().findPaymentsByStatuses(statuses);
        then(adminPaymentService).shouldHaveNoInteractions();
    }

    @Test
    void findPaymentTotalForPeriod_isUser_delegatesToUserService() {
        // given
        given(authenticationService.isAdmin()).willReturn(false);
        given(authenticationService.isUser()).willReturn(true);

        Instant start = Instant.now();
        Instant end = Instant.now().plusSeconds(60);

        // action
        decorator.findPaymentTotalForPeriod(start, end);

        // assertThat
        then(userPaymentService).should().findPaymentTotalForPeriod(start, end);
        then(adminPaymentService).shouldHaveNoInteractions();
    }
}