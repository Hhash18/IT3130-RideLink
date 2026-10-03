package com.ridelink.ride.dto;


import java.math.BigDecimal;
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record FareQuote(BigDecimal total, String currency) {}
