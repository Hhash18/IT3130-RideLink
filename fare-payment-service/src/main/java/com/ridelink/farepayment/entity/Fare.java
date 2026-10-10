package com.ridelink.farepayment.entity;

import com.ridelink.farepayment.dto.FareBreakdown;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.dto.RideSnapshot;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="fares")
public class Fare {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=100) private String rideId;
    @Column(nullable=false, length=100) private String passengerId;
    @Column(nullable=false, length=200) private String pickup;
    @Column(nullable=false, length=200) private String destination;
    @Column(nullable=false, precision=10, scale=3) private BigDecimal distanceKm;
    @Column(nullable=false) private int durationMinutes;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal baseFare;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal distanceCharge;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal timeCharge;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal total;
    @Column(nullable=false, length=3) private String currency;
    @Column(nullable=false, length=40) private String ruleVersion;
    @Column(nullable=false) private Instant createdAt;

    protected Fare() {}

    public UUID getId() { return id; }
    public String getRideId() { return rideId; }
    public String getPickup() { return pickup; }
    public String getDestination() { return destination; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getBaseFare() { return baseFare; }
    public BigDecimal getDistanceCharge() { return distanceCharge; }
    public BigDecimal getTimeCharge() { return timeCharge; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getRuleVersion() { return ruleVersion; }
    public Instant getCreatedAt() { return createdAt; }

    public Fare(RideSnapshot ride, FareBreakdown breakdown) {
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
