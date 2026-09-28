package com.ridelink.farepayment;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {}
    public enum PaymentMethod { CASH, MOCK_CARD }
    public enum Simulation { SUCCESS, DECLINED }
    public enum PaymentStatus { SUCCEEDED, FAILED }
    public enum RidePaymentStatus { UNPAID, FAILED, PAID }

    public record EstimateRequest(
            @NotBlank @Size(max=200) String pickup,
            @NotBlank @Size(max=200) String destination,
            @NotNull @DecimalMin("0.001") @DecimalMax("1000") @Digits(integer=4, fraction=3)
            @Schema(example="10.000", description="Simulated distance in km; no maps integration") BigDecimal distanceKm,
            @NotNull @Min(0) @Max(1440) Integer durationMinutes) {}

    public record FareBreakdown(BigDecimal baseFare, BigDecimal distanceCharge,
            BigDecimal timeCharge, BigDecimal total, String currency, String ruleVersion) {}
    public record EstimateResponse(String pickup, String destination, BigDecimal distanceKm,
            int durationMinutes, FareBreakdown fare) {}
    public record FareResponse(UUID id, String rideId, String passengerId, String pickup,
            String destination, BigDecimal distanceKm, int durationMinutes,
            FareBreakdown fare, Instant createdAt) {}
    public record PaymentRequest(@NotNull PaymentMethod method, @NotNull Simulation simulation) {}
    public record PaymentResponse(UUID id, String rideId, UUID idempotencyKey,
            PaymentMethod method, PaymentStatus status, BigDecimal amount, String currency,
            String failureReason, String receiptNumber, Instant createdAt) {}
    public record PaymentSummary(String rideId, RidePaymentStatus status, List<PaymentResponse> attempts) {}
    public record ReceiptResponse(String receiptNumber, UUID paymentId, String rideId,
            String passengerId, String pickup, String destination, FareBreakdown fare,
            PaymentMethod method, Instant paidAt, boolean simulated) {}
    public record ApiError(Instant timestamp, int status, String code, String message,
            String path, List<String> details) {}
}
