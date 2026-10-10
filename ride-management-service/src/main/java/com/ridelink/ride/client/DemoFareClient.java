package com.ridelink.ride.client;


import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.ServiceException;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Component
@Profile("demo")
public class DemoFareClient implements FareClient {
    private final RideRepository rides;
    public DemoFareClient(RideRepository rides) { this.rides=rides; }
    private FareQuote quote(BigDecimal distance,int minutes) {
        return new FareQuote(new BigDecimal("100.00").add(distance.multiply(new BigDecimal("60.00")).setScale(2,RoundingMode.HALF_UP))
                .add(new BigDecimal("5.00").multiply(BigDecimal.valueOf(minutes))),"LKR");
    }
    public FareQuote estimate(RideRequest request) { return quote(request.estimatedDistanceKm(),request.estimatedDurationMinutes()); }
    public FinalFareResponse finalizeFare(UUID rideId) {
        var ride=rides.findById(rideId).orElseThrow(() -> ServiceException.notFound("Ride not found")).response();
        return new FinalFareResponse(UUID.nameUUIDFromBytes(("demo-fare-"+rideId).getBytes(StandardCharsets.UTF_8)),rideId.toString(),quote(ride.actualDistanceKm(),ride.actualDurationMinutes()));
    }
}
