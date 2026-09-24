package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.VehicleRequest;
import com.ridelink.drivervehicle.dto.VehicleResponse;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleResponse> getAllVehicles() {

        return vehicleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VehicleResponse getVehicleById(Long id) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found with id: " + id));

        return mapToResponse(vehicle);
    }

    public VehicleResponse createVehicle(VehicleRequest request) {

        Vehicle vehicle = new Vehicle();

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setType(request.getType());
        vehicle.setModel(request.getModel());
        vehicle.setCapacity(request.getCapacity());
        vehicle.setStatus(request.getStatus());

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return mapToResponse(savedVehicle);
    }

    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {

        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found with id: " + id));

        existingVehicle.setRegistrationNumber(
                request.getRegistrationNumber()
        );
        existingVehicle.setType(request.getType());
        existingVehicle.setModel(request.getModel());
        existingVehicle.setCapacity(request.getCapacity());
        existingVehicle.setStatus(request.getStatus());

        Vehicle updatedVehicle = vehicleRepository.save(existingVehicle);

        return mapToResponse(updatedVehicle);
    }

    public void deleteVehicle(Long id) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found with id: " + id));

        vehicleRepository.delete(vehicle);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getType(),
                vehicle.getModel(),
                vehicle.getCapacity(),
                vehicle.getStatus()
        );
    }
}