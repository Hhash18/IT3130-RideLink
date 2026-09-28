package com.ridelink.farepayment;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ridelink.farepayment.ApiModels.*;

@Service
public class FareService {
    private final FareRepository fares;
    private final RideClient rides;
    private final FareCalculator calculator;
    private final AccessControl access;
    public FareService(FareRepository fares, RideClient rides, FareCalculator calculator, AccessControl access) {
        this.fares = fares; this.rides = rides; this.calculator = calculator; this.access = access;
    }
    public EstimateResponse estimate(EstimateRequest request) {
        return new EstimateResponse(request.pickup(), request.destination(), request.distanceKm(),
                request.durationMinutes(), calculator.calculate(request.distanceKm(), request.durationMinutes()));
    }
    @Transactional
    public FareResponse finalizeFare(String rideId) {
        // PUT is idempotent: a finalized fare is an immutable price/ownership snapshot.
        var existing = fares.findByRideId(rideId);
        if (existing.isPresent()) return existing.get().response();
        var ride = rides.getRide(rideId);
        if (ride == null || !rideId.equals(ride.id()) || !validText(ride.passengerId(), 100)
                || !validText(ride.pickup(), 200) || !validText(ride.destination(), 200) || ride.status() == null)
            throw invalidRide();
        if (!"COMPLETED".equals(ride.status()))
            throw ServiceException.conflict("RIDE_NOT_COMPLETED", "Only a completed ride can have a final fare");
        FareBreakdown breakdown;
        try { breakdown = calculator.calculate(ride.actualDistanceKm(), ride.actualDurationMinutes()); }
        catch (IllegalArgumentException ex) { throw invalidRide(); }
        return fares.saveAndFlush(new Fare(ride, breakdown)).response();
    }
    @Transactional(readOnly=true)
    public FareResponse get(String rideId) {
        Fare fare = fares.findByRideId(rideId).orElseThrow(() -> ServiceException.notFound("Final fare not found"));
        access.requireOwnerOrPrivileged(fare.passengerId);
        return fare.response();
    }
    private boolean validText(String value, int max) { return value != null && !value.isBlank() && value.length() <= max; }
    private ServiceException invalidRide() {
        return new ServiceException(HttpStatus.BAD_GATEWAY, "INVALID_RIDE_RESPONSE", "Ride service returned invalid trip data");
    }
}
