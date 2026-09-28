package com.ridelink.farepayment.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
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
import java.util.Date;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:jwt-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class JwtSecurityApiTest {
    static RSAKey key;
    static HttpServer server;
    static {
        try {
            key=new RSAKeyGenerator(2048).keyID("test-key").generate();
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/jwks", exchange -> {
                byte[] body=new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(200,body.length);
                try(var out=exchange.getResponseBody()) { out.write(body); }
            });
            server.start();
        } catch(Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry properties) {
        properties.add("ridelink.security.jwk-set-uri", () -> "http://127.0.0.1:"+server.getAddress().getPort()+"/jwks");
        properties.add("ridelink.security.issuer", () -> "https://account.test");
    }
    @AfterAll static void stop() { server.stop(0); }
    @Autowired MockMvc mvc;
    String token(String issuer, String audience, String role, Instant expiry, RSAKey signingKey) throws Exception {
        var claims=new JWTClaimsSet.Builder().subject("passenger-001").issuer(issuer).audience(audience)
                .claim("roles",List.of(role)).issueTime(Date.from(Instant.now().minusSeconds(600)));
        if(expiry!=null) claims.expirationTime(Date.from(expiry));
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),claims.build());
        jwt.sign(new RSASSASigner(signingKey));return jwt.serialize();
    }
    @Test void acceptsAccountSignedTokenAndAppliesRole() throws Exception {
        String valid=token("https://account.test","ridelink","PASSENGER",Instant.now().plusSeconds(300),key);
        mvc.perform(post("/api/fares/estimate").header("Authorization","Bearer "+valid)
                .contentType("application/json").content("{\"pickup\":\"A\",\"destination\":\"B\",\"distanceKm\":10,\"durationMinutes\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fare.total").value(800));
        mvc.perform(put("/api/fares/rides/r1").header("Authorization","Bearer "+valid)).andExpect(status().isForbidden());
        String driver=token("https://account.test","ridelink","DRIVER",Instant.now().plusSeconds(300),key);
        mvc.perform(get("/api/fares/rides/r1").header("Authorization","Bearer "+driver)).andExpect(status().isForbidden());
    }
    @Test void rejectsWrongIssuerAudienceExpiryAndSignature() throws Exception {
        for(String token : List.of(
                token("https://wrong.test","ridelink","PASSENGER",Instant.now().plusSeconds(300),key),
                token("https://account.test","wrong","PASSENGER",Instant.now().plusSeconds(300),key),
                token("https://account.test","ridelink","PASSENGER",Instant.now().minusSeconds(300),key),
                token("https://account.test","ridelink","PASSENGER",null,key),
                token("https://account.test","ridelink","PASSENGER",Instant.now().plusSeconds(300),new RSAKeyGenerator(2048).generate()))) {
            mvc.perform(get("/api/fares/rides/r1").header("Authorization","Bearer "+token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }
    @Test void integrationOpenApiUsesBearerAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}
