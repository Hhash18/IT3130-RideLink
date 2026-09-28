package com.ridelink.drivervehicle;

import com.ridelink.drivervehicle.dto.VehicleRequest;
import com.ridelink.drivervehicle.dto.VehicleResponse;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.entity.VehicleStatus;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import com.ridelink.drivervehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle vehicle;
    private VehicleRequest request;

    @BeforeEach
    void setUp() {

        vehicle = new Vehicle();

        vehicle.setId(1L);
        vehicle.setRegistrationNumber("CAB-1234");
        vehicle.setType("Car");
        vehicle.setModel("Toyota Prius");
        vehicle.setCapacity(4);
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        request = new VehicleRequest();

        request.setRegistrationNumber("CAB-1234");
        request.setType("Car");
        request.setModel("Toyota Prius");
        request.setCapacity(4);
        request.setStatus("AVAILABLE");
    }

    @Test
    void getAllVehicles_shouldReturnVehicles() {

        when(vehicleRepository.findAll())
                .thenReturn(List.of(vehicle));

        List<VehicleResponse> result =
                vehicleService.getAllVehicles();

        assertEquals(1, result.size());
        assertEquals("CAB-1234",
                result.get(0).getRegistrationNumber());

        verify(vehicleRepository).findAll();
    }

    @Test
    void getAvailableVehicles_shouldReturnAvailableVehicles() {

        when(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE))
                .thenReturn(List.of(vehicle));

        List<VehicleResponse> result =
                vehicleService.getAvailableVehicles();

        assertEquals(1, result.size());
        assertEquals(VehicleStatus.AVAILABLE.name(),
                result.get(0).getStatus());

        verify(vehicleRepository)
                .findByStatus(VehicleStatus.AVAILABLE);
    }

    @Test
    void getVehicleById_shouldReturnVehicle() {

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        VehicleResponse result =
                vehicleService.getVehicleById(1L);

        assertEquals(1L, result.getId());
        assertEquals("CAB-1234",
                result.getRegistrationNumber());

        verify(vehicleRepository).findById(1L);
    }

    @Test
    void getVehicleById_shouldThrowExceptionWhenNotFound() {

        when(vehicleRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.getVehicleById(99L)
        );

        verify(vehicleRepository).findById(99L);
    }

    @Test
    void createVehicle_shouldCreateVehicle() {

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenReturn(vehicle);

        VehicleResponse result =
                vehicleService.createVehicle(request);

        assertNotNull(result);
        assertEquals("CAB-1234",
                result.getRegistrationNumber());
        assertEquals("AVAILABLE",
                result.getStatus());

        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void updateVehicle_shouldUpdateVehicle() {

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenReturn(vehicle);

        request.setModel("Honda Vezel");
        request.setCapacity(5);

        VehicleResponse result =
                vehicleService.updateVehicle(1L, request);

        assertNotNull(result);

        verify(vehicleRepository).findById(1L);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void updateVehicle_shouldThrowExceptionWhenNotFound() {

        when(vehicleRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.updateVehicle(99L, request)
        );

        verify(vehicleRepository).findById(99L);
        verify(vehicleRepository, never())
                .save(any(Vehicle.class));
    }

    @Test
    void deleteVehicle_shouldDeleteVehicle() {

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle(1L);

        verify(vehicleRepository).findById(1L);
        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteVehicle_shouldThrowExceptionWhenNotFound() {

        when(vehicleRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.deleteVehicle(99L)
        );

        verify(vehicleRepository).findById(99L);
        verify(vehicleRepository, never())
                .delete(any(Vehicle.class));
    }
}