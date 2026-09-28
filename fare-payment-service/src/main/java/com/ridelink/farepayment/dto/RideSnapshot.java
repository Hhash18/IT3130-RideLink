package com.ridelink.farepayment.dto;

import java.math.BigDecimal;
public record RideSnapshot(String id, String passengerId, String status, String pickup,
        String destination, BigDecimal actualDistanceKm, Integer actualDurationMinutes) {}
