package com.ridelink.drivervehicle.repository;

import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByStatus(DriverStatus status);
}