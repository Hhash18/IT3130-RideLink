package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + id));
    }

    public Driver createDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    public Driver updateDriver(Long id, Driver updatedDriver) {

        Driver existingDriver = getDriverById(id);

        existingDriver.setName(updatedDriver.getName());
        existingDriver.setLicenseNumber(updatedDriver.getLicenseNumber());
        existingDriver.setPhone(updatedDriver.getPhone());
        existingDriver.setEmail(updatedDriver.getEmail());
        existingDriver.setStatus(updatedDriver.getStatus());

        return driverRepository.save(existingDriver);
    }

    public void deleteDriver(Long id) {

        Driver driver = getDriverById(id);

        driverRepository.delete(driver);
    }
}