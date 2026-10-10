package com.ridelink.ride.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown=true)
public record FinalFareResponse(UUID id, String rideId, FareQuote fare) {}
