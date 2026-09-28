package com.ridelink.farepayment;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ridelink.farepayment.ApiModels.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByFareIdAndIdempotencyKey(UUID fareId, UUID idempotencyKey);
    boolean existsByFareIdAndStatus(UUID fareId, PaymentStatus status);
    List<Payment> findByFareIdOrderByCreatedAtAscIdAsc(UUID fareId);
}
