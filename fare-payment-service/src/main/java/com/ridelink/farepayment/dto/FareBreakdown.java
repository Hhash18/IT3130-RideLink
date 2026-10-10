package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FareBreakdown(BigDecimal baseFare, BigDecimal distanceCharge,
        BigDecimal timeCharge, BigDecimal total, String currency, String ruleVersion) {}
