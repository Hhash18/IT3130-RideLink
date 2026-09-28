package com.ridelink.farepayment;


import java.math.BigDecimal;

/** The Ride service remains the authority for ownership, completion and actual trip measurements. */
public interface RideClient {
    RideSnapshot getRide(String rideId);
    record RideSnapshot(String id, String passengerId, String status, String pickup,
            String destination, BigDecimal actualDistanceKm, Integer actualDurationMinutes) {}
}
