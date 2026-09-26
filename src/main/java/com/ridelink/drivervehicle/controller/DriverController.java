package com.ridelink.drivervehicle.controller;

import com.ridelink.drivervehicle.dto.DriverAvailabilityRequest;
import com.ridelink.drivervehicle.dto.DriverRequest;
import com.ridelink.drivervehicle.dto.DriverResponse;
import com.ridelink.drivervehicle.entity.DriverStatus;
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

    // Get all drivers
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {

        return ResponseEntity.ok(
                driverService.getAllDrivers()
        );
    }

    // Get available drivers
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {

        return ResponseEntity.ok(
                driverService.getAvailableDrivers()
        );
    }

    // Get driver by ID
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                driverService.getDriverById(id)
        );
    }

    // Create driver
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody DriverRequest request) {

        DriverResponse createdDriver =
                driverService.createDriver(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdDriver);
    }

    // Update driver
    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequest request) {

        return ResponseEntity.ok(
                driverService.updateDriver(id, request)
        );
    }

    // Update driver availability
    @PatchMapping("/{id}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DriverAvailabilityRequest request) {

        return ResponseEntity.ok(
                driverService.updateAvailability(
                        id,
                        request.getStatus()
                )
        );
    }

    // Delete driver
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable Long id) {

        driverService.deleteDriver(id);

        return ResponseEntity.noContent().build();
    }
}