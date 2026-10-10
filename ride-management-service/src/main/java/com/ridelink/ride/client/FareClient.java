package com.ridelink.ride.client;


import com.ridelink.ride.dto.*;
import java.util.UUID;
public interface FareClient {
    FareQuote estimate(RideRequest request);
    FinalFareResponse finalizeFare(UUID rideId);
}
