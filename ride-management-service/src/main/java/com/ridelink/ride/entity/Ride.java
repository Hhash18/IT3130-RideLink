package com.ridelink.ride.entity;


import com.ridelink.ride.dto.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="rides")
public class Ride {
    @Id private UUID id;
    @Column(nullable=false,length=100) private String passengerId;
    @Column(nullable=false,length=200) private String pickup;
    @Column(nullable=false,length=200) private String destination;
    @Column(nullable=false,length=100) private String serviceArea;
    @Column(nullable=false,precision=10,scale=3) private BigDecimal estimatedDistanceKm;
    @Column(nullable=false) private int estimatedDurationMinutes;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal estimatedFare;
    @Column(nullable=false,length=3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private RideStatus status;
    private Long driverId;
    @Column(length=254) private String driverEmail;
    private Long vehicleId;
    @Column(unique=true) private Long activeDriverId;
    @Column(unique=true) private Long activeVehicleId;
    @Column(precision=10,scale=3) private BigDecimal actualDistanceKm;
    private Integer actualDurationMinutes;
    @Column(length=300) private String cancellationReason;
    private UUID finalFareId;
    @Column(precision=12,scale=2) private BigDecimal finalFare;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    protected Ride() {}
    public Ride(String passengerId,RideRequest request,FareQuote quote) {
        id=UUID.randomUUID();this.passengerId=passengerId;pickup=request.pickup().trim();destination=request.destination().trim();
        serviceArea=request.serviceArea().trim();estimatedDistanceKm=request.estimatedDistanceKm();
        estimatedDurationMinutes=request.estimatedDurationMinutes();estimatedFare=quote.total();currency=quote.currency();
        status=RideStatus.REQUESTED;createdAt=Instant.now();updatedAt=createdAt;
    }
    public UUID getId() { return id; }
    public String getPassengerId() { return passengerId; }
    public String getDriverEmail() { return driverEmail; }
    public String getServiceArea() { return serviceArea; }
    public RideStatus getStatus() { return status; }
    public void assign(DriverResponse driver) {
        driverId=driver.id();driverEmail=driver.email();vehicleId=driver.vehicleId();
        activeDriverId=driver.id();activeVehicleId=driver.vehicleId();status=RideStatus.ASSIGNED;
        assignedAt=Instant.now();updatedAt=assignedAt;
    }
    public void accept() { status=RideStatus.ACCEPTED;acceptedAt=Instant.now();updatedAt=acceptedAt; }
    public void start() { status=RideStatus.IN_PROGRESS;startedAt=Instant.now();updatedAt=startedAt; }
    public void complete(CompleteRideRequest request) {
        actualDistanceKm=request.actualDistanceKm();actualDurationMinutes=request.actualDurationMinutes();
        status=RideStatus.COMPLETED;completedAt=Instant.now();updatedAt=completedAt;release();
    }
    public void cancel(String reason) { cancellationReason=reason.trim();status=RideStatus.CANCELLED;cancelledAt=Instant.now();updatedAt=cancelledAt;release(); }
    private void release() { activeDriverId=null;activeVehicleId=null; }
    public void recordFare(FinalFareResponse response) { finalFareId=response.id();finalFare=response.fare().total();updatedAt=Instant.now(); }
    public RideResponse response() {
        return new RideResponse(id,passengerId,pickup,destination,serviceArea,estimatedDistanceKm,estimatedDurationMinutes,
            estimatedFare,currency,status,driverId,vehicleId,actualDistanceKm,actualDurationMinutes,cancellationReason,
            finalFareId,finalFare,createdAt,updatedAt,assignedAt,acceptedAt,startedAt,completedAt,cancelledAt);
    }
}
