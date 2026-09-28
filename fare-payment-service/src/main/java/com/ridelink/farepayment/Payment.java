package com.ridelink.farepayment;


import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import static com.ridelink.farepayment.ApiModels.*;

@Entity
@Table(name="payments", uniqueConstraints=@UniqueConstraint(columnNames={"fare_id", "idempotency_key"}))
public class Payment {
    @Id UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="fare_id") Fare fare;
    @Column(nullable=false) UUID idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) PaymentStatus status;
    @Column(length=100) String failureReason;
    @Column(unique=true, length=50) String receiptNumber;
    @Column(nullable=false) Instant createdAt;

    protected Payment() {}
    Payment(Fare fare, UUID key, PaymentRequest request) {
        id = UUID.randomUUID(); this.fare = fare; idempotencyKey = key;
        method = request.method(); createdAt = Instant.now();
        status = request.simulation() == Simulation.SUCCESS ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        if (status == PaymentStatus.SUCCEEDED) receiptNumber = "RL-" + id;
        else failureReason = "SIMULATED_DECLINE";
    }
    boolean matches(PaymentRequest request) {
        return method == request.method() && (status == PaymentStatus.SUCCEEDED) == (request.simulation() == Simulation.SUCCESS);
    }
    PaymentResponse response() {
        FareResponse snapshot = fare.response();
        return new PaymentResponse(id, snapshot.rideId(), idempotencyKey, method, status, snapshot.fare().total(),
                snapshot.fare().currency(), failureReason, receiptNumber, createdAt);
    }
}
