package com.ridelink.farepayment;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface FareRepository extends JpaRepository<Fare, UUID> {
    Optional<Fare> findByRideId(String rideId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fare f where f.rideId = :rideId")
    Optional<Fare> lockByRideId(String rideId);
}
