package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.DriverRequest;
import com.ridelink.drivervehicle.dto.DriverResponse;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.entity.VehicleStatus;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    
    public DriverService(
        DriverRepository driverRepository,
        VehicleRepository vehicleRepository
    ) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // Get all drivers
    public List<DriverResponse> getAllDrivers() {

        return driverRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get available drivers
    public List<DriverResponse> getAvailableDrivers() {

        return driverRepository.findAll()
                .stream()
                .filter(driver ->
                        driver.getStatus() == DriverStatus.AVAILABLE
                )
                .map(this::mapToResponse)
                .toList();
    }

    // Get driver by ID
    public DriverResponse getDriverById(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found with id: " + id
                        )
                );

        return mapToResponse(driver);
    }

    // Create driver
    public DriverResponse createDriver(DriverRequest request) {

        Driver driver = new Driver();

        driver.setName(request.getName());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setPhone(request.getPhone());
        driver.setEmail(request.getEmail());

        // DriverRequest already uses DriverStatus enum
        driver.setStatus(request.getStatus());

        Driver savedDriver = driverRepository.save(driver);

        return mapToResponse(savedDriver);
    }

    // Update driver
    public DriverResponse updateDriver(
            Long id,
            DriverRequest request
    ) {

        Driver existingDriver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found with id: " + id
                        )
                );

        existingDriver.setName(request.getName());
        existingDriver.setLicenseNumber(request.getLicenseNumber());
        existingDriver.setPhone(request.getPhone());
        existingDriver.setEmail(request.getEmail());

        // DriverRequest already uses DriverStatus enum
        existingDriver.setStatus(request.getStatus());

        Driver updatedDriver =
                driverRepository.save(existingDriver);

        return mapToResponse(updatedDriver);
    }

    // Update driver availability/status
    public DriverResponse updateAvailability(
            Long id,
            DriverStatus status
    ) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found with id: " + id
                        )
                );

        driver.setStatus(status);

        Driver updatedDriver =
                driverRepository.save(driver);

        return mapToResponse(updatedDriver);
    }

    // Assign vehicle to driver
    public DriverResponse assignVehicle(
        Long driverId,
        Long vehicleId
    ) {
        
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found with id: " + driverId
                        )
               );
               
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                               "Vehicle not found with id: " + vehicleId
                       )
                );
                
                if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                        throw new IllegalStateException(
                                "Vehicle is not available for assignment"
                        );
                }

    driver.setVehicle(vehicle);

    Driver updatedDriver =
            driverRepository.save(driver);

    return mapToResponse(updatedDriver);
}

    // Delete driver
    public void deleteDriver(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found with id: " + id
                        )
                );

        driverRepository.delete(driver);
    }

    // Entity -> Response DTO
    private DriverResponse mapToResponse(Driver driver) {

        DriverResponse response = new DriverResponse();

        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setPhone(driver.getPhone());
        response.setEmail(driver.getEmail());

        // DriverResponse also uses DriverStatus enum
        response.setStatus(driver.getStatus());

        if (driver.getVehicle() != null) {
                response.setVehicleId(
                        driver.getVehicle().getId()
                );
                
                response.setVehicleRegistrationNumber(
                        driver.getVehicle().getRegistrationNumber()
                );
        }

        return response;
    }
}