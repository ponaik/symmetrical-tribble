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

    @Operation(
            summary = "Create a new payment",
            description = "Initiates a payment. Users can only create payments for their own IDs; admins can create payments for any user."
    )
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentFacade.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update payment status",
            description = "Updates a payment's status. Users are restricted to their own payments; admins have global update authority."
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        PaymentResponse response = paymentFacade.updatePaymentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a payment",
            description = "Permanently removes a payment record. Users can only delete their own payments; admins can delete any record."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentFacade.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Find payments by order ID",
            description = "Retrieves payments for a specific order. Users see only their associated records; admins see all records for the order."
    )
    @GetMapping("/by-order")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByOrderId(@RequestParam Long orderId) {
        List<PaymentResponse> payments = paymentFacade.findPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    @Operation(
            summary = "Find payments by user ID",
            description = "Lists payments for a user. Standard users must provide their own ID; admins can query any user's ID."
    )
    @GetMapping("/by-user")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByUserId(@RequestParam Long userId) {
        List<PaymentResponse> payments = paymentFacade.findPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    @Operation(
            summary = "Find payments by status",
            description = "Filters payments by status. Users receive their matching records; admins receive all system-wide matching records."
    )
    @GetMapping("/by-status")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByStatuses(@RequestParam List<PaymentStatus> statuses) {
        List<PaymentResponse> payments = paymentFacade.findPaymentsByStatuses(statuses);
        return ResponseEntity.ok(payments);
    }

    @Operation(
            summary = "Calculate payment total",
            description = "Aggregates payment amounts for a period. Users see their personal total; admins see the total system revenue."
    )
    @GetMapping("/total")
    public ResponseEntity<PaymentTotalResponse> findPaymentTotalForPeriod(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        PaymentTotalResponse total = paymentFacade.findPaymentTotalForPeriod(start, end);
        return ResponseEntity.ok(total);
    }
}