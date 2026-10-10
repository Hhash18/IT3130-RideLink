package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.RidePaymentStatus;
import jakarta.validation.constraints.*;
import java.util.List;

public record PaymentSummary(String rideId, RidePaymentStatus status, List<PaymentResponse> attempts) {}
