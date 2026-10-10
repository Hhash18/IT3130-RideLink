package com.ridelink.ride.client;


import com.ridelink.ride.dto.DriverResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
@Profile("!demo")
public class RestDriverClient implements DriverClient {
    private final IntegrationHttp http;
    public RestDriverClient(@Value("${ridelink.driver-service-url}") String baseUrl) { http=new IntegrationHttp(baseUrl,"Driver"); }
    public List<DriverResponse> available() { return Arrays.asList(http.exchange(HttpMethod.GET,"/api/drivers/available",null,DriverResponse[].class)); }
}
