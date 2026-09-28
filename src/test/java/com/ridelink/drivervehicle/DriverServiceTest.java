package com.ridelink.drivervehicle;

import com.ridelink.drivervehicle.dto.DriverRequest;
import com.ridelink.drivervehicle.dto.DriverResponse;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.entity.VehicleStatus;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import com.ridelink.drivervehicle.service.DriverService;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    private Driver driver;
    private Vehicle vehicle;
    private DriverRequest request;

    @BeforeEach
    void setUp() {

        driver = new Driver(
                "Test Driver",
                "B1234567",
                "0771234567",
                "test@gmail.com",
                DriverStatus.AVAILABLE
        );

        driver.setId(1L);

        vehicle = new Vehicle(
                "CAB-1234",
                "Car",
                "Toyota Prius",
                4,
                VehicleStatus.AVAILABLE
        );

        vehicle.setId(1L);

        request = new DriverRequest();

        request.setName("Updated Driver");
        request.setLicenseNumber("B7654321");
        request.setPhone("0712345678");
        request.setEmail("updated@gmail.com");
        request.setStatus(DriverStatus.ON_TRIP);
    }

    // ---------------------------------------------------------
    // GET ALL DRIVERS
    // ---------------------------------------------------------

    @Test
    void getAllDrivers_shouldReturnAllDrivers() {

        when(driverRepository.findAll())
                .thenReturn(List.of(driver));

        List<DriverResponse> result =
                driverService.getAllDrivers();

        assertEquals(1, result.size());
        assertEquals("Test Driver", result.get(0).getName());
        assertEquals(DriverStatus.AVAILABLE, result.get(0).getStatus());

        verify(driverRepository, times(1)).findAll();
    }

    // ---------------------------------------------------------
    // GET AVAILABLE DRIVERS
    // ---------------------------------------------------------

    @Test
    void getAvailableDrivers_shouldReturnOnlyAvailableDrivers() {

        Driver unavailableDriver = new Driver(
                "Unavailable Driver",
                "B9999999",
                "0711111111",
                "unavailable@gmail.com",
                DriverStatus.UNAVAILABLE
        );

        unavailableDriver.setId(2L);

        when(driverRepository.findAll())
                .thenReturn(List.of(driver, unavailableDriver));

        List<DriverResponse> result =
                driverService.getAvailableDrivers();

        assertEquals(1, result.size());
        assertEquals("Test Driver", result.get(0).getName());
        assertEquals(
                DriverStatus.AVAILABLE,
                result.get(0).getStatus()
        );

        verify(driverRepository, times(1)).findAll();
    }

    // ---------------------------------------------------------
    // GET DRIVER BY ID
    // ---------------------------------------------------------

    @Test
    void getDriverById_shouldReturnDriver() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        DriverResponse result =
                driverService.getDriverById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Test Driver", result.getName());
        assertEquals("B1234567", result.getLicenseNumber());
        assertEquals("0771234567", result.getPhone());
        assertEquals(DriverStatus.AVAILABLE, result.getStatus());

        verify(driverRepository, times(1))
                .findById(1L);
    }

    // ---------------------------------------------------------
    // GET DRIVER BY ID - NOT FOUND
    // ---------------------------------------------------------

    @Test
    void getDriverById_shouldThrowExceptionWhenNotFound() {

        when(driverRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.getDriverById(99L)
        );

        verify(driverRepository, times(1))
                .findById(99L);
    }

    // ---------------------------------------------------------
    // CREATE DRIVER
    // ---------------------------------------------------------

    @Test
    void createDriver_shouldCreateAndReturnDriver() {

        when(driverRepository.save(any(Driver.class)))
                .thenReturn(driver);

        DriverResponse result =
                driverService.createDriver(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Driver", result.getName());
        assertEquals(DriverStatus.AVAILABLE, result.getStatus());

        verify(driverRepository, times(1))
                .save(any(Driver.class));
    }

    // ---------------------------------------------------------
    // UPDATE DRIVER
    // ---------------------------------------------------------

    @Test
    void updateDriver_shouldUpdateAndReturnDriver() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(driverRepository.save(any(Driver.class)))
                .thenReturn(driver);

        DriverResponse result =
                driverService.updateDriver(1L, request);

        assertNotNull(result);
        assertEquals("Updated Driver", result.getName());
        assertEquals("B7654321", result.getLicenseNumber());
        assertEquals("0712345678", result.getPhone());
        assertEquals("updated@gmail.com", result.getEmail());
        assertEquals(DriverStatus.ON_TRIP, result.getStatus());

        verify(driverRepository, times(1))
                .findById(1L);

        verify(driverRepository, times(1))
                .save(driver);
    }

    // ---------------------------------------------------------
    // UPDATE DRIVER - NOT FOUND
    // ---------------------------------------------------------

    @Test
    void updateDriver_shouldThrowExceptionWhenNotFound() {

        when(driverRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.updateDriver(99L, request)
        );

        verify(driverRepository, never())
                .save(any(Driver.class));
    }

    // ---------------------------------------------------------
    // UPDATE AVAILABILITY
    // ---------------------------------------------------------

    @Test
    void updateAvailability_shouldUpdateStatus() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(driverRepository.save(any(Driver.class)))
                .thenReturn(driver);

        DriverResponse result =
                driverService.updateAvailability(
                        1L,
                        DriverStatus.ON_TRIP
                );

        assertEquals(
                DriverStatus.ON_TRIP,
                result.getStatus()
        );

        verify(driverRepository, times(1))
                .findById(1L);

        verify(driverRepository, times(1))
                .save(driver);
    }

    // ---------------------------------------------------------
    // UPDATE AVAILABILITY - NOT FOUND
    // ---------------------------------------------------------

    @Test
    void updateAvailability_shouldThrowExceptionWhenNotFound() {

        when(driverRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.updateAvailability(
                        99L,
                        DriverStatus.AVAILABLE
                )
        );

        verify(driverRepository, never())
                .save(any(Driver.class));
    }

    // ---------------------------------------------------------
    // ASSIGN VEHICLE
    // ---------------------------------------------------------

    @Test
    void assignVehicle_shouldAssignAvailableVehicle() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        when(driverRepository.save(any(Driver.class)))
                .thenReturn(driver);

        DriverResponse result =
                driverService.assignVehicle(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getVehicleId());
        assertEquals(
                "CAB-1234",
                result.getVehicleRegistrationNumber()
        );

        verify(driverRepository, times(1))
                .findById(1L);

        verify(vehicleRepository, times(1))
                .findById(1L);

        verify(driverRepository, times(1))
                .save(driver);
    }

    // ---------------------------------------------------------
    // ASSIGN VEHICLE - VEHICLE NOT AVAILABLE
    // ---------------------------------------------------------

    @Test
    void assignVehicle_shouldThrowExceptionWhenVehicleUnavailable() {

        vehicle.setStatus(VehicleStatus.UNAVAILABLE);

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        assertThrows(
                IllegalStateException.class,
                () -> driverService.assignVehicle(1L, 1L)
        );

        verify(driverRepository, never())
                .save(any(Driver.class));
    }

    // ---------------------------------------------------------
    // ASSIGN VEHICLE - DRIVER NOT FOUND
    // ---------------------------------------------------------

    @Test
    void assignVehicle_shouldThrowExceptionWhenDriverNotFound() {

        when(driverRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.assignVehicle(99L, 1L)
        );

        verify(vehicleRepository, never())
                .findById(anyLong());
    }

    // ---------------------------------------------------------
    // ASSIGN VEHICLE - VEHICLE NOT FOUND
    // ---------------------------------------------------------

    @Test
    void assignVehicle_shouldThrowExceptionWhenVehicleNotFound() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(vehicleRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.assignVehicle(1L, 99L)
        );

        verify(driverRepository, never())
                .save(any(Driver.class));
    }

    // ---------------------------------------------------------
    // DELETE DRIVER
    // ---------------------------------------------------------

    @Test
    void deleteDriver_shouldDeleteDriver() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        doNothing()
                .when(driverRepository)
                .delete(driver);

        driverService.deleteDriver(1L);

        verify(driverRepository, times(1))
                .findById(1L);

        verify(driverRepository, times(1))
                .delete(driver);
    }

    // ---------------------------------------------------------
    // DELETE DRIVER - NOT FOUND
    // ---------------------------------------------------------

    @Test
    void deleteDriver_shouldThrowExceptionWhenNotFound() {

        when(driverRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.deleteDriver(99L)
        );

        verify(driverRepository, never())
                .delete(any(Driver.class));
    }
}