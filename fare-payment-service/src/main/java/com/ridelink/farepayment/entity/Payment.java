package com.ridelink.farepayment.entity;

import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="payments", uniqueConstraints=@UniqueConstraint(columnNames={"fare_id", "idempotency_key"}))
public class Payment {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="fare_id") private Fare fare;
    @Column(nullable=false) private UUID idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private PaymentStatus status;
    @Column(length=100) private String failureReason;
    @Column(unique=true, length=50) private String receiptNumber;
    @Column(nullable=false) private Instant createdAt;

    protected Payment() {}

    public UUID getId() { return id; }
    public Fare getFare() { return fare; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getReceiptNumber() { return receiptNumber; }
    public Instant getCreatedAt() { return createdAt; }

    public Payment(Fare fare, UUID key, PaymentRequest request) {
        id = UUID.randomUUID(); this.fare = fare; idempotencyKey = key;
        method = request.method(); createdAt = Instant.now();
        status = request.simulation() == Simulation.SUCCESS ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        if (status == PaymentStatus.SUCCEEDED) receiptNumber = "RL-" + id;
        else failureReason = "SIMULATED_DECLINE";
    }
    public boolean matches(PaymentRequest request) {
        return method == request.method() && (status == PaymentStatus.SUCCEEDED) == (request.simulation() == Simulation.SUCCESS);
    }
    public PaymentResponse response() {
        FareResponse snapshot = fare.response();
        return new PaymentResponse(id, snapshot.rideId(), idempotencyKey, method, status, snapshot.fare().total(),
                snapshot.fare().currency(), failureReason, receiptNumber, createdAt);
    }
}
