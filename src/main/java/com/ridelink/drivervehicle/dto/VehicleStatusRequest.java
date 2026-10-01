package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public class VehicleStatusRequest {

    @NotNull(message = "Vehicle status is required")
    private VehicleStatus status;

    public VehicleStatusRequest() {
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
}