package com.intern.paymentservice.controller;

import com.intern.paymentservice.dto.CreatePaymentRequest;
import com.intern.paymentservice.dto.PaymentResponse;
import com.intern.paymentservice.dto.PaymentTotalResponse;
import com.intern.paymentservice.dto.UpdatePaymentStatusRequest;
import com.intern.paymentservice.model.PaymentStatus;
import com.intern.paymentservice.service.PaymentFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@NullMarked
@Tag(name = "Payments", description = "Payment management API")
@RestController
@RequestMapping("/api/payments")
@Validated
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;

    /**
     * Creates a new payment.
     * Maps to POST /api/payments
     * @param request The payment creation request body.
     * @return 201 Created with the created PaymentResponse.
     */
    @Operation(summary = "Create a new payment")
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentFacade.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the status of an existing payment.
     * Maps to PATCH /api/payments/{id}/status
     * @param id The ID of the payment.
     * @param request The status update request body.
     * @return 200 OK with the updated PaymentResponse.
     */
    @Operation(summary = "Update the status of an existing payment")
    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        PaymentResponse response = paymentFacade.updatePaymentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a payment by its ID.
     * Maps to DELETE /api/payments/{id}
     * @param id The ID of the payment to delete.
     * @return 204 No Content.
     */
    @Operation(summary = "Delete a payment")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentFacade.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Finds payments associated with a specific order ID.
     * Maps to GET /api/payments/by-order?orderId={orderId}
     * @param orderId The ID of the order.
     * @return 200 OK with a list of payments.
     */
    @Operation(summary = "Find payments by order ID")
    @GetMapping("/by-order")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByOrderId(@RequestParam Long orderId) {
        List<PaymentResponse> payments = paymentFacade.findPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Finds payments associated with a specific user ID.
     * Maps to GET /api/payments/by-user?userId={userId}
     * @param userId The ID of the user.
     * @return 200 OK with a list of payments.
     */
    @Operation(summary = "Find payments by user ID")
    @GetMapping("/by-user")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByUserId(@RequestParam Long userId) {
        List<PaymentResponse> payments = paymentFacade.findPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Finds payments matching a list of statuses.
     * Maps to GET /api/payments/by-status?statuses=COMPLETED,PENDING
     * @param statuses A list of payment statuses to filter by.
     * @return 200 OK with a list of payments.
     */
    @Operation(summary = "Find payments by one or more statuses")
    @GetMapping("/by-status")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByStatuses(@RequestParam List<PaymentStatus> statuses) {
        List<PaymentResponse> payments = paymentFacade.findPaymentsByStatuses(statuses);
        return ResponseEntity.ok(payments);
    }

    /**
     * Calculates the total payment amount for a given time period.
     * Maps to GET /api/payments/total?start={start}&end={end}
     * @param start The start timestamp of the period (ISO format).
     * @param end The end timestamp of the period (ISO format).
     * @return 200 OK with the total BigDecimal amount.
     */
    @Operation(summary = "Calculate total payment amount for a period")
    @GetMapping("/total")
    public ResponseEntity<PaymentTotalResponse> findPaymentTotalForPeriod(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        PaymentTotalResponse total = paymentFacade.findPaymentTotalForPeriod(start, end);
        return ResponseEntity.ok(total);
    }
}