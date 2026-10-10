package com.ridelink.ride.dto;


import jakarta.validation.constraints.*;
public record CancelRideRequest(@NotBlank @Size(max=300) String reason) {}
