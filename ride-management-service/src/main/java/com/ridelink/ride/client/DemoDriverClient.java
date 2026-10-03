package com.ridelink.ride.client;


import com.ridelink.ride.dto.DriverResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@Profile("demo")
public class DemoDriverClient implements DriverClient {
    public List<DriverResponse> available() {
        return List.of(new DriverResponse(1L,"driver@example.test","AVAILABLE","Colombo",101L));
    }
}
