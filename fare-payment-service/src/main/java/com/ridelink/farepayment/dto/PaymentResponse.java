package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.PaymentMethod;
import com.ridelink.farepayment.entity.PaymentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, String rideId, UUID idempotencyKey,
        PaymentMethod method, PaymentStatus status, BigDecimal amount, String currency,
        String failureReason, String receiptNumber, Instant createdAt) {}
