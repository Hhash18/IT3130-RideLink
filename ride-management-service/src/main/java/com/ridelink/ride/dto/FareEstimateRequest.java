package com.ridelink.ride.dto;


import java.math.BigDecimal;
public record FareEstimateRequest(String pickup, String destination, BigDecimal distanceKm, int durationMinutes) {}
