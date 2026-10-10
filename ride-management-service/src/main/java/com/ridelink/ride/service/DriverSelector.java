package com.ridelink.ride.service;


import com.ridelink.ride.dto.DriverResponse;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class DriverSelector {
    /** Deterministic lowest driver ID among available profiles with matching area and a vehicle. */
    public List<DriverResponse> eligible(List<DriverResponse> drivers,String area) {
        return drivers.stream().filter(Objects::nonNull)
            .filter(d -> d.id()!=null && d.id()>0 && d.vehicleId()!=null && d.vehicleId()>0)
            .filter(d -> d.email()!=null && !d.email().isBlank() && d.email().length()<=254)
            .filter(d -> "AVAILABLE".equals(d.status()) && d.serviceArea()!=null && area.trim().equalsIgnoreCase(d.serviceArea().trim()))
            .sorted(Comparator.comparing(DriverResponse::id)).toList();
    }
}
