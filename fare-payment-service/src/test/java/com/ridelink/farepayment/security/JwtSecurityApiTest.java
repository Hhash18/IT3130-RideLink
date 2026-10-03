package com.ridelink.farepayment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.farepayment.repository.FareRepository;
import com.ridelink.farepayment.repository.PaymentRepository;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:jwt-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class JwtSecurityApiTest {
    // Random per test run; no application signing secret is checked into source control.
    static final String SECRET = UUID.randomUUID().toString().replace("-", "");
    static final AtomicReference<String> forwardedToken = new AtomicReference<>();
    static final HttpServer server;
    static {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/rides/ride-123", exchange -> {
                forwardedToken.set(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] body = ("{\"id\":\"ride-123\",\"passengerId\":\"42\",\"status\":\"COMPLETED\","
                        + "\"pickup\":\"A\",\"destination\":\"B\",\"actualDistanceKm\":10,\"actualDurationMinutes\":20}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var out = exchange.getResponseBody()) { out.write(body); }
            });
            server.start();
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry properties) {
        properties.add("ridelink.security.jwt-secret", () -> SECRET);
        properties.add("ridelink.ride-service-url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
    }
    @AfterAll static void stop() { server.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PaymentRepository payments;
    @Autowired FareRepository fares;
    @BeforeEach void clean() { payments.deleteAll(); fares.deleteAll(); forwardedToken.set(null); }

    String token(String secret, String subject, Object role, Instant expiry) {
        // Same builder and claim names as Account JwtService, including absence of issuer/audience.
        var builder = Jwts.builder().subject(subject).claim("email", "passenger@example.test")
                .claim("role", role).issuedAt(Date.from(Instant.now().minusSeconds(600)));
        if (expiry != null) builder.expiration(Date.from(expiry));
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    String valid(String subject, String role) { return token(SECRET, subject, role, Instant.now().plusSeconds(300)); }

    @Test void acceptsAccountTokenWithoutIssuerAudienceAndAppliesSingularRole() throws Exception {
        String passenger = valid("42", "PASSENGER");
        mvc.perform(post("/api/fares/estimate").header("Authorization", "Bearer " + passenger)
                .contentType("application/json").content("{\"pickup\":\"A\",\"destination\":\"B\",\"distanceKm\":10,\"durationMinutes\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fare.total").value(800));
        mvc.perform(put("/api/fares/rides/ride-123").header("Authorization", "Bearer " + passenger)).andExpect(status().isForbidden());
        mvc.perform(get("/api/fares/rides/ride-123").header("Authorization", "Bearer " + valid("9", "DRIVER"))).andExpect(status().isForbidden());
    }
    @Test void adminFinalizesWithForwardedAccountTokenAndOwnerCanPayAndReadReceipt() throws Exception {
        String admin = valid("1", "ADMIN");
        mvc.perform(put("/api/fares/rides/ride-123").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passengerId").value("42"));
        assertThat(forwardedToken.get()).isEqualTo("Bearer " + admin);
        String owner = valid("42", "PASSENGER");
        String body = mvc.perform(post("/api/payments/rides/ride-123").header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID()).contentType("application/json")
                .content("{\"method\":\"MOCK_CARD\",\"simulation\":\"SUCCESS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        mvc.perform(get("/api/payments/" + id + "/receipt").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fare.total").value(800));
        for (String path : List.of("/api/fares/rides/ride-123", "/api/payments/" + id, "/api/payments/" + id + "/receipt")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + valid("43", "PASSENGER"))).andExpect(status().isForbidden());
        }
    }
    @Test void rejectsBadSignatureExpiryMissingIdentityAndMalformedTokens() throws Exception {
        for (String invalid : List.of(
                token(UUID.randomUUID().toString().replace("-", ""), "42", "PASSENGER", Instant.now().plusSeconds(300)),
                token(SECRET, "42", "PASSENGER", Instant.now().minusSeconds(300)),
                token(SECRET, "42", "PASSENGER", null),
                token(SECRET, "", "PASSENGER", Instant.now().plusSeconds(300)),
                "not-a-jwt")) {
            mvc.perform(get("/api/fares/rides/ride-123").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }
    @Test void missingOrUnrecognizedRoleCannotAccessBusinessEndpoints() throws Exception {
        for (String role : Arrays.asList(null, "UNKNOWN", "ROLE_ADMIN")) {
            mvc.perform(get("/api/fares/rides/ride-123")
                    .header("Authorization", "Bearer " + token(SECRET, "42", role, Instant.now().plusSeconds(300))))
                    .andExpect(status().isForbidden());
        }
    }
    @Test void integrationOpenApiUsesBearerAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}
