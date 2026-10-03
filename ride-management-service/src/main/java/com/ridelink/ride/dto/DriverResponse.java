package com.ridelink.ride.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown=true)
public record DriverResponse(Long id, String email, String status, String serviceArea, Long vehicleId) {}
