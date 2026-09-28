package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.ApiError;
import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.PaymentSummary;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@ApiResponses({
        @ApiResponse(responseCode="400", description="Invalid input or missing required header", content=@Content(schema=@Schema(implementation=ApiError.class))),
        @ApiResponse(responseCode="401", description="Missing or invalid authentication", content=@Content(schema=@Schema(implementation=ApiError.class))),
        @ApiResponse(responseCode="403", description="Role or ownership does not permit this operation", content=@Content(schema=@Schema(implementation=ApiError.class)))
})
@Tag(name="Payments", description="Member 4 service. All payments are simulated.")
@PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN', 'SERVICE')")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments = payments; }

    @ApiResponses({
            @ApiResponse(responseCode="404", description="Final fare not found", content=@Content(schema=@Schema(implementation=ApiError.class))),
            @ApiResponse(responseCode="409", description="Already paid, conflicting idempotency key, or concurrent update", content=@Content(schema=@Schema(implementation=ApiError.class)))
    })
    @PostMapping("/payments/rides/{rideId}")
    @Operation(summary="Record a simulated payment", description="Required UUID Idempotency-Key is scoped to this ride. HTTP 200 returns a recorded SUCCEEDED or FAILED result, including replays. A retry after FAILED must use a new key. The amount comes from the stored final fare.")
    public PaymentResponse pay(@PathVariable @Pattern(regexp="[A-Za-z0-9_-]{1,100}") String rideId,
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody PaymentRequest request) {
        return payments.pay(rideId, key, request);
    }
    @GetMapping("/payments/rides/{rideId}")
    @Operation(summary="Get payment status and attempt history for a ride")
    public PaymentSummary status(@PathVariable @Pattern(regexp="[A-Za-z0-9_-]{1,100}") String rideId) { return payments.status(rideId); }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary="Retrieve a simulated payment")
    public PaymentResponse payment(@PathVariable UUID paymentId) { return payments.get(paymentId); }

    @ApiResponses({
            @ApiResponse(responseCode="404", description="Payment not found", content=@Content(schema=@Schema(implementation=ApiError.class))),
            @ApiResponse(responseCode="409", description="Payment did not succeed", content=@Content(schema=@Schema(implementation=ApiError.class)))
    })
    @GetMapping("/payments/{paymentId}/receipt")
    @Operation(summary="Retrieve the JSON receipt for a successful payment")
    public ReceiptResponse receipt(@PathVariable UUID paymentId) { return payments.receipt(paymentId); }
}
