package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;

public class DriverAvailabilityRequest {

    @NotNull(message = "Driver status is required")
    private DriverStatus status;

    public DriverAvailabilityRequest() {
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }
}