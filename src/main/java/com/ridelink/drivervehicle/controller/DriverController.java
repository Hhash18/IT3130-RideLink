package com.ridelink.drivervehicle.controller;

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
    public ResponseEntity<List<Driver>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PostMapping
    public ResponseEntity<Driver> createDriver(
            @Valid @RequestBody Driver driver) {

        Driver createdDriver = driverService.createDriver(driver);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdDriver);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody Driver driver) {

        return ResponseEntity.ok(
                driverService.updateDriver(id, driver)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable Long id) {

        driverService.deleteDriver(id);

        return ResponseEntity.noContent().build();
    }
}