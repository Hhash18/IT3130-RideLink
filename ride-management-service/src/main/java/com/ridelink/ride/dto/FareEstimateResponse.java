package com.ridelink.ride.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown=true)
public record FareEstimateResponse(FareQuote fare) {}
