package com.ridelink.farepayment;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class RestRideClientTest {
    HttpServer server;
    RestRideClient client;
    int status;
    String response;
    AtomicReference<String> authorization = new AtomicReference<>();
    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/rides/r1", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status,bytes.length);
            try (var out=exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        client = new RestRideClient("http://127.0.0.1:"+server.getAddress().getPort());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("test-token").header("alg","RS256").subject("ride-service").build()));
    }
    @AfterEach void cleanup() { server.stop(0);SecurityContextHolder.clearContext(); }
    @Test void fetchesRideAndPropagatesBearerToken() {
        status=200;response="{\"id\":\"r1\",\"passengerId\":\"p1\",\"status\":\"COMPLETED\",\"pickup\":\"A\",\"destination\":\"B\",\"actualDistanceKm\":10,\"actualDurationMinutes\":20}";
        assertThat(client.getRide("r1").actualDistanceKm()).isEqualByComparingTo("10");
        assertThat(authorization.get()).isEqualTo("Bearer test-token");
    }
    @Test void mapsDependencyFailures() {
        response="{}";
        for (int code : new int[]{404,403,500}) {
            status=code;
            String expected=code==404 ? "NOT_FOUND" : code==403 ? "RIDE_ACCESS_DENIED" : "RIDE_SERVICE_UNAVAILABLE";
            assertThatThrownBy(() -> client.getRide("r1")).isInstanceOfSatisfying(ServiceException.class,
                    ex -> assertThat(ex.code).isEqualTo(expected));
        }
    }
    @Test void mapsInvalidJsonAndConnectionFailure() {
        status=200;response="not-json";
        assertThatThrownBy(() -> client.getRide("r1")).isInstanceOf(ServiceException.class);
        server.stop(0);
        assertThatThrownBy(() -> client.getRide("r1")).isInstanceOfSatisfying(ServiceException.class,
                ex -> assertThat(ex.code).isEqualTo("RIDE_SERVICE_UNAVAILABLE"));
    }
}
