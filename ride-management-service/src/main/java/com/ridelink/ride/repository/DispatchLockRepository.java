package com.ridelink.ride.repository;


import com.ridelink.ride.entity.DispatchLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface DispatchLockRepository extends JpaRepository<DispatchLock,Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DispatchLock d where d.id=1")
    DispatchLock acquire();
}
