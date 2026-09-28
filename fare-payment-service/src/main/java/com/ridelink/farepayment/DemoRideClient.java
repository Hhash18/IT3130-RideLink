package com.ridelink.farepayment;


import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Fixed demonstration fixtures, never used in the integration profile. Not a Ride service implementation. */
@Component
@Profile("demo")
public class DemoRideClient implements RideClient {
    @Override
    public RideSnapshot getRide(String rideId) {
        return switch (rideId) {
            case "ride-demo-001" -> ride(rideId, "passenger-001", "COMPLETED");
            case "ride-demo-002" -> ride(rideId, "passenger-002", "COMPLETED");
            case "ride-demo-active" -> ride(rideId, "passenger-001", "IN_PROGRESS");
            default -> throw ServiceException.notFound("Demo ride not found");
        };
    }
    private RideSnapshot ride(String id, String passenger, String status) {
        return new RideSnapshot(id, passenger, status, "Colombo Fort", "Bambalapitiya", new BigDecimal("10.000"), 20);
    }
}
