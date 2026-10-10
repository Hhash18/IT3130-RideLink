package com.ridelink.ride.client;


import com.ridelink.ride.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
@Profile("!demo")
public class RestFareClient implements FareClient {
    private final IntegrationHttp http;
    public RestFareClient(@Value("${ridelink.fare-service-url}") String baseUrl) { http=new IntegrationHttp(baseUrl,"Fare"); }
    public FareQuote estimate(RideRequest request) {
        var body=new FareEstimateRequest(request.pickup(),request.destination(),request.estimatedDistanceKm(),request.estimatedDurationMinutes());
        return http.exchange(HttpMethod.POST,"/api/fares/estimate",body,FareEstimateResponse.class).fare();
    }
    public FinalFareResponse finalizeFare(UUID rideId) {
        return http.exchange(HttpMethod.PUT,"/api/fares/rides/"+rideId,null,FinalFareResponse.class);
    }
}
