package com.ridelink.farepayment.client;

import com.ridelink.farepayment.dto.RideSnapshot;
import com.ridelink.farepayment.exception.ServiceException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("!demo")
public class RestRideClient implements RideClient {
    private final RestClient client;
    public RestRideClient(@Value("${ridelink.ride-service-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
    @Override
    public RideSnapshot getRide(String rideId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "TOKEN_REQUIRED", "A bearer token is required");
        }
        try {
            var ride = client.get().uri("/api/rides/{id}", rideId)
                    .headers(headers -> headers.setBearerAuth(jwt.getToken().getTokenValue()))
                    .retrieve().body(RideSnapshot.class);
            if (ride == null) throw new ServiceException(HttpStatus.BAD_GATEWAY, "INVALID_RIDE_RESPONSE", "Ride service returned no ride");
            return ride;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw ServiceException.notFound("Ride not found");
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403)
                throw new ServiceException(HttpStatus.FORBIDDEN, "RIDE_ACCESS_DENIED", "Ride service denied access");
            throw unavailable();
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }
    private ServiceException unavailable() {
        return new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "RIDE_SERVICE_UNAVAILABLE", "Ride service unavailable; retry later");
    }
}
