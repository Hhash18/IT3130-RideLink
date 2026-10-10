package com.ridelink.ride.service;


import com.ridelink.ride.client.FareClient;
import com.ridelink.ride.dto.RideResponse;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class FareIntegrationService {
    private final RideService rides;
    private final FareClient fares;
    public FareIntegrationService(RideService rides,FareClient fares) { this.rides=rides;this.fares=fares; }
    // Deliberately no surrounding transaction: Fare calls GET /api/rides/{id} back on this service.
    public RideResponse finalizeFare(UUID id) {
        var ride=rides.completedForBilling(id);
        if(ride.finalFareId()!=null) return ride;
        return rides.recordFare(id,fares.finalizeFare(id));
    }
}
