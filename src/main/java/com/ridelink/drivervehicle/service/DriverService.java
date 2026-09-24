package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.DriverRequest;
import com.ridelink.drivervehicle.dto.DriverResponse;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.exception.DriverNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<DriverResponse> getAllDrivers() {

        return driverRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DriverResponse getDriverById(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new DriverNotFoundException(
                                "Driver not found with id: " + id
                        ));

        return mapToResponse(driver);
    }

    public DriverResponse createDriver(DriverRequest request) {

        Driver driver = new Driver();

        driver.setName(request.getName());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setPhone(request.getPhone());
        driver.setEmail(request.getEmail());
        driver.setStatus(request.getStatus());

        Driver savedDriver = driverRepository.save(driver);

        return mapToResponse(savedDriver);
    }

    public DriverResponse updateDriver(
            Long id,
            DriverRequest request) {

        Driver existingDriver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new DriverNotFoundException(
                                "Driver not found with id: " + id
                        ));

        existingDriver.setName(request.getName());
        existingDriver.setLicenseNumber(request.getLicenseNumber());
        existingDriver.setPhone(request.getPhone());
        existingDriver.setEmail(request.getEmail());
        existingDriver.setStatus(request.getStatus());

        Driver updatedDriver =
                driverRepository.save(existingDriver);

        return mapToResponse(updatedDriver);
    }

    public void deleteDriver(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new DriverNotFoundException(
                                "Driver not found with id: " + id
                        ));

        driverRepository.delete(driver);
    }

    private DriverResponse mapToResponse(Driver driver) {

        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getLicenseNumber(),
                driver.getPhone(),
                driver.getEmail(),
                driver.getStatus()
        );
    }
}