package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(String receiptNumber, UUID paymentId, String rideId,
        String passengerId, String pickup, String destination, FareBreakdown fare,
        PaymentMethod method, Instant paidAt, boolean simulated) {}
