package com.ridelink.ride.dto;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CompleteRideRequest(
    @NotNull @DecimalMin("0.001") @DecimalMax("1000") @Digits(integer=4,fraction=3) BigDecimal actualDistanceKm,
    @NotNull @Min(0) @Max(1440) Integer actualDurationMinutes) {}
