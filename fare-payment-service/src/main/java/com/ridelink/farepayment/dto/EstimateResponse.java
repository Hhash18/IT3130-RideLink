package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EstimateResponse(String pickup, String destination, BigDecimal distanceKm,
        int durationMinutes, FareBreakdown fare) {}
