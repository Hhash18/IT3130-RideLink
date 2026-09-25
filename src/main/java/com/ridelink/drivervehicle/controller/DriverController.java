package com.ridelink.drivervehicle.controller;

import com.ridelink.drivervehicle.dto.DriverRequest;
import com.ridelink.drivervehicle.dto.DriverResponse;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.service.DriverService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {

        List<Driver> drivers = driverService.getAllDrivers();

        List<DriverResponse> response = drivers.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        List<Driver> drivers = driverService.getAvailableDrivers();
        List<DriverResponse> response = drivers.stream()
        .map(this::toResponse)
        .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id) {

        Driver driver = driverService.getDriverById(id);

        return ResponseEntity.ok(toResponse(driver));
    }

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody DriverRequest request) {

        Driver driver = toEntity(request);

        Driver createdDriver = driverService.createDriver(driver);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(createdDriver));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequest request) {

        Driver driver = toEntity(request);

        Driver updatedDriver =
                driverService.updateDriver(id, driver);

        return ResponseEntity.ok(toResponse(updatedDriver));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable Long id) {

        driverService.deleteDriver(id);

        return ResponseEntity.noContent().build();
    }

    private Driver toEntity(DriverRequest request) {

        Driver driver = new Driver();

        driver.setName(request.getName());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setPhone(request.getPhone());
        driver.setEmail(request.getEmail());

        /*
         * DriverRequest status type eka current project eke
         * String nam enum ekata convert karanawa.
         */
        driver.setStatus(
                com.ridelink.drivervehicle.entity.DriverStatus
                        .valueOf(request.getStatus().toUpperCase())
        );

        return driver;
    }

    private DriverResponse toResponse(Driver driver) {

        DriverResponse response = new DriverResponse();

        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setPhone(driver.getPhone());
        response.setEmail(driver.getEmail());
        response.setStatus(driver.getStatus().name());

        return response;
    }
}