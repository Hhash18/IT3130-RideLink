package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.FareBreakdown;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareCalculator {
    public static final String RULE_VERSION = "LKR-STANDARD-v1";
    private static final BigDecimal BASE = new BigDecimal("100.00");
    private static final BigDecimal PER_KM = new BigDecimal("60.00");
    private static final BigDecimal PER_MINUTE = new BigDecimal("5.00");

    public FareBreakdown calculate(BigDecimal distanceKm, Integer durationMinutes) {
        if (distanceKm == null || distanceKm.compareTo(new BigDecimal("0.001")) < 0
                || distanceKm.compareTo(new BigDecimal("1000")) > 0
                || distanceKm.stripTrailingZeros().scale() > 3
                || durationMinutes == null || durationMinutes < 0 || durationMinutes > 1440) {
            throw new IllegalArgumentException("Distance must be 0.001-1000 km (up to 3 decimals) and duration 0-1440 minutes");
        }
        BigDecimal distance = distanceKm.multiply(PER_KM).setScale(2, RoundingMode.HALF_UP);
        BigDecimal time = PER_MINUTE.multiply(BigDecimal.valueOf(durationMinutes)).setScale(2, RoundingMode.HALF_UP);
        return new FareBreakdown(BASE, distance, time, BASE.add(distance).add(time), "LKR", RULE_VERSION);
    }
}
