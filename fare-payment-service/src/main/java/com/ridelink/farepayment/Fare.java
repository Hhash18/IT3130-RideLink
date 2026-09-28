package com.ridelink.farepayment;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static com.ridelink.farepayment.ApiModels.*;

@Entity
@Table(name="fares")
public class Fare {
    @Id UUID id;
    @Column(nullable=false, unique=true, length=100) String rideId;
    @Column(nullable=false, length=100) String passengerId;
    @Column(nullable=false, length=200) String pickup;
    @Column(nullable=false, length=200) String destination;
    @Column(nullable=false, precision=10, scale=3) BigDecimal distanceKm;
    @Column(nullable=false) int durationMinutes;
    @Column(nullable=false, precision=12, scale=2) BigDecimal baseFare;
    @Column(nullable=false, precision=12, scale=2) BigDecimal distanceCharge;
    @Column(nullable=false, precision=12, scale=2) BigDecimal timeCharge;
    @Column(nullable=false, precision=12, scale=2) BigDecimal total;
    @Column(nullable=false, length=3) String currency;
    @Column(nullable=false, length=40) String ruleVersion;
    @Column(nullable=false) Instant createdAt;

    protected Fare() {}
    Fare(RideClient.RideSnapshot ride, FareBreakdown breakdown) {
        id = UUID.randomUUID(); rideId = ride.id(); passengerId = ride.passengerId();
        pickup = ride.pickup(); destination = ride.destination();
        distanceKm = ride.actualDistanceKm(); durationMinutes = ride.actualDurationMinutes();
        baseFare = breakdown.baseFare(); distanceCharge = breakdown.distanceCharge();
        timeCharge = breakdown.timeCharge(); total = breakdown.total();
        currency = breakdown.currency(); ruleVersion = breakdown.ruleVersion(); createdAt = Instant.now();
    }
    public String getPassengerId() { return passengerId; }
    public FareBreakdown breakdown() {
        return new FareBreakdown(baseFare, distanceCharge, timeCharge, total, currency, ruleVersion);
    }
    public FareResponse response() {
        return new FareResponse(id, rideId, passengerId, pickup, destination, distanceKm,
                durationMinutes, breakdown(), createdAt);
    }
}
