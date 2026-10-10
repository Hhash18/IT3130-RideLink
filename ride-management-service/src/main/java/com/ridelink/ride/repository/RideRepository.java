package com.ridelink.ride.repository;


import com.ridelink.ride.entity.Ride;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface RideRepository extends JpaRepository<Ride,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id=:id")
    Optional<Ride> lockById(UUID id);
    boolean existsByActiveDriverIdOrActiveVehicleId(Long driverId,Long vehicleId);
    Page<Ride> findByPassengerId(String passengerId,Pageable pageable);
    Page<Ride> findByDriverEmailIgnoreCase(String driverEmail,Pageable pageable);
}
