package com.ridelink.ride.client;

import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.ServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
class RestClientsTest {
    HttpServer server;String base;int status;String response;
    AtomicReference<String> path=new AtomicReference<>(),body=new AtomicReference<>(),auth=new AtomicReference<>(),method=new AtomicReference<>();
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange -> {
            path.set(exchange.getRequestURI().getPath());method.set(exchange.getRequestMethod());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status,bytes.length);try(var out=exchange.getResponseBody()) { out.write(bytes); }
        });server.start();base="http://127.0.0.1:"+server.getAddress().getPort();status=200;
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test-token").header("alg","HS256").subject("1").build()));
    }
    @AfterEach void stop() { server.stop(0);SecurityContextHolder.clearContext(); }
    @Test void driverContractAndBearerPropagation() {
        response="[{\"id\":1,\"email\":\"driver@example.test\",\"status\":\"AVAILABLE\",\"serviceArea\":\"Colombo\",\"vehicleId\":101,\"name\":\"Demo\"}]";
        assertThat(new RestDriverClient(base).available()).hasSize(1);
        assertThat(path.get()).isEqualTo("/api/drivers/available");assertThat(auth.get()).isEqualTo("Bearer test-token");
    }
    @Test void fareEstimateAndFinalizationMatchMemberFourContract() {
        var client=new RestFareClient(base);
        response="{\"fare\":{\"total\":800,\"currency\":\"LKR\",\"baseFare\":100},\"pickup\":\"A\"}";
        assertThat(client.estimate(new RideRequest("A","B","Colombo",BigDecimal.TEN,20)).total()).isEqualByComparingTo("800");
        assertThat(path.get()).isEqualTo("/api/fares/estimate");assertThat(body.get()).contains("\"distanceKm\":10","\"durationMinutes\":20");
        UUID rideId=UUID.randomUUID();UUID fareId=UUID.randomUUID();
        response="{\"id\":\""+fareId+"\",\"rideId\":\""+rideId+"\",\"fare\":{\"total\":800,\"currency\":\"LKR\"}}";
        assertThat(client.finalizeFare(rideId).id()).isEqualTo(fareId);assertThat(method.get()).isEqualTo("PUT");
        assertThat(path.get()).isEqualTo("/api/fares/rides/"+rideId);
    }
    @Test void errorsAreMappedWithoutLeakingUpstreamBodies() {
        response="{\"secret\":\"do-not-leak\"}";
        for(int code:new int[]{401,403,409,500}) {
            status=code;
            assertThatThrownBy(() -> new RestDriverClient(base).available()).isInstanceOf(ServiceException.class).hasMessageNotContaining("do-not-leak");
        }
        status=200;response="invalid-json";
        assertThatThrownBy(() -> new RestDriverClient(base).available()).isInstanceOf(ServiceException.class);
        server.stop(0);
        assertThatThrownBy(() -> new RestDriverClient(base).available()).isInstanceOf(ServiceException.class);
    }
}
