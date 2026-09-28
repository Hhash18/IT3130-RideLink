package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.PaymentSummary;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.entity.RidePaymentStatus;
import com.ridelink.farepayment.exception.ServiceException;
import com.ridelink.farepayment.repository.FareRepository;
import com.ridelink.farepayment.repository.PaymentRepository;
import com.ridelink.farepayment.security.AccessControl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class PaymentService {
    private final FareRepository fares;
    private final PaymentRepository payments;
    private final AccessControl access;
    public PaymentService(FareRepository fares, PaymentRepository payments, AccessControl access) {
        this.fares = fares; this.payments = payments; this.access = access;
    }
    @Transactional
    public PaymentResponse pay(String rideId, UUID idempotencyKey, PaymentRequest request) {
        // Serializes attempts per fare, including across application instances using the same database.
        Fare fare = fares.lockByRideId(rideId).orElseThrow(() -> ServiceException.notFound("Final fare not found"));
        access.requireOwnerOrPrivileged(fare.getPassengerId());
        var previous = payments.findByFareIdAndIdempotencyKey(fare.getId(), idempotencyKey);
        if (previous.isPresent()) {
            if (!previous.get().matches(request)) throw ServiceException.conflict("IDEMPOTENCY_CONFLICT", "This key was already used for a different request");
            return previous.get().response();
        }
        if (payments.existsByFareIdAndStatus(fare.getId(), PaymentStatus.SUCCEEDED))
            throw ServiceException.conflict("ALREADY_PAID", "This ride has already been paid");
        return payments.saveAndFlush(new Payment(fare, idempotencyKey, request)).response();
    }
    @Transactional(readOnly=true)
    public PaymentSummary status(String rideId) {
        Fare fare = fares.findByRideId(rideId).orElseThrow(() -> ServiceException.notFound("Final fare not found"));
        access.requireOwnerOrPrivileged(fare.getPassengerId());
        var attempts = payments.findByFareIdOrderByCreatedAtAscIdAsc(fare.getId()).stream().map(Payment::response).toList();
        RidePaymentStatus status = attempts.stream().anyMatch(p -> p.status() == PaymentStatus.SUCCEEDED)
                ? RidePaymentStatus.PAID : (attempts.isEmpty() ? RidePaymentStatus.UNPAID : RidePaymentStatus.FAILED);
        return new PaymentSummary(rideId, status, attempts);
    }
    @Transactional(readOnly=true)
    public PaymentResponse get(UUID id) { return authorizedPayment(id).response(); }
    @Transactional(readOnly=true)
    public ReceiptResponse receipt(UUID id) {
        Payment payment = authorizedPayment(id);
        if (payment.getStatus() != PaymentStatus.SUCCEEDED)
            throw ServiceException.conflict("PAYMENT_NOT_SUCCESSFUL", "A failed payment has no receipt");
        FareResponse fare = payment.getFare().response();
        return new ReceiptResponse(payment.getReceiptNumber(), payment.getId(), fare.rideId(), fare.passengerId(),
                fare.pickup(), fare.destination(), fare.fare(), payment.getMethod(), payment.getCreatedAt(), true);
    }
    private Payment authorizedPayment(UUID id) {
        Payment payment = payments.findById(id).orElseThrow(() -> ServiceException.notFound("Payment not found"));
        access.requireOwnerOrPrivileged(payment.getFare().getPassengerId());
        return payment;
    }
}
