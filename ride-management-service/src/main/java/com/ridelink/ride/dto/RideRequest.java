package com.ridelink.ride.dto;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record RideRequest(
    @NotBlank @Size(max=200) String pickup,
    @NotBlank @Size(max=200) String destination,
    @NotBlank @Size(max=100) String serviceArea,
    @NotNull @DecimalMin("0.001") @DecimalMax("1000") @Digits(integer=4,fraction=3) BigDecimal estimatedDistanceKm,
    @NotNull @Min(0) @Max(1440) Integer estimatedDurationMinutes) {}
