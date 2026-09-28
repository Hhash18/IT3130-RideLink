package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message,
        String path, List<String> details) {}
