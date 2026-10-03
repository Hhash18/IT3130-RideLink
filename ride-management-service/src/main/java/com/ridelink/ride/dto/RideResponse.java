package com.ridelink.ride.dto;


import com.ridelink.ride.entity.RideStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record RideResponse(UUID id, String passengerId, String pickup, String destination,
    String serviceArea, BigDecimal estimatedDistanceKm, int estimatedDurationMinutes,
    BigDecimal estimatedFare, String currency, RideStatus status, Long driverId, Long vehicleId,
    BigDecimal actualDistanceKm, Integer actualDurationMinutes, String cancellationReason,
    UUID finalFareId, BigDecimal finalFare, Instant createdAt, Instant updatedAt,
    Instant assignedAt, Instant acceptedAt, Instant startedAt, Instant completedAt, Instant cancelledAt) {}
