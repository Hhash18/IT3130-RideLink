package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.PaymentMethod;
import com.ridelink.farepayment.entity.Simulation;
import jakarta.validation.constraints.*;

public record PaymentRequest(@NotNull PaymentMethod method, @NotNull Simulation simulation) {}
