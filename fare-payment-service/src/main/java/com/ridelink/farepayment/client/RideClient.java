package com.ridelink.farepayment.client;

import com.ridelink.farepayment.dto.RideSnapshot;


/** The Ride service remains the authority for ownership, completion and actual trip measurements. */
public interface RideClient {
    RideSnapshot getRide(String rideId);
}
