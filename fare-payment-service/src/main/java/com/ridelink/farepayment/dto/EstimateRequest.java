package com.ridelink.farepayment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EstimateRequest(
        @NotBlank @Size(max=200) String pickup,
        @NotBlank @Size(max=200) String destination,
        @NotNull @DecimalMin("0.001") @DecimalMax("1000") @Digits(integer=4, fraction=3)
        @Schema(example="10.000", description="Simulated distance in km; no maps integration") BigDecimal distanceKm,
        @NotNull @Min(0) @Max(1440) Integer durationMinutes) {}
