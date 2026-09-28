package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FareResponse(UUID id, String rideId, String passengerId, String pickup,
        String destination, BigDecimal distanceKm, int durationMinutes,
        FareBreakdown fare, Instant createdAt) {}
