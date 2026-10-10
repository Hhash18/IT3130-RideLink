package com.ridelink.ride.controller;

import com.fasterxml.jackson.databind.*;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.ServiceException;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles({"demo","test"})
class RideApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired RideRepository repository;
    @Autowired RideService service;
    @BeforeEach void clean() { repository.deleteAll(); }
    JsonNode create(String passenger,String area) throws Exception {
        var request=new RideRequest("Colombo Fort","Bambalapitiya",area,new java.math.BigDecimal("10.000"),20);
        return mapper.readTree(mvc.perform(post("/api/rides").with(user(passenger).roles("PASSENGER"))
            .contentType("application/json").content(mapper.writeValueAsString(request)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED"))
            .andExpect(jsonPath("$.estimatedFare").value(800)).andExpect(header().exists("Location"))
            .andReturn().getResponse().getContentAsString());
    }
    ResultActions action(String id,String operation,String username,String role,String body) throws Exception {
        var request=put("/api/rides/"+id+"/"+operation).with(user(username).roles(role));
        if(body!=null) request.contentType("application/json").content(body);
        return mvc.perform(request);
    }
    void assign(String id) throws Exception { action(id,"assignment","passenger-001","PASSENGER",null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED")); }
    void start(String id) throws Exception {
        assign(id);
        action(id,"acceptance","driver@example.test","DRIVER",null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        action(id,"start","driver@example.test","DRIVER",null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
    @Test void fullLifecycleAndFareCallbackContract() throws Exception {
        String id=create("passenger-001","Colombo").get("id").asText();start(id);
        action(id,"completion","driver@example.test","DRIVER","{\"actualDistanceKm\":12,\"actualDurationMinutes\":25}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/rides/"+id).with(user("fare-service").roles("SERVICE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.passengerId").value("passenger-001"))
            .andExpect(jsonPath("$.actualDistanceKm").value(12)).andExpect(jsonPath("$.actualDurationMinutes").value(25));
        var result=action(id,"fare","admin-demo","ADMIN",null).andExpect(status().isOk())
            .andExpect(jsonPath("$.finalFare").value(945)).andReturn().getResponse().getContentAsString();
        String fareId=mapper.readTree(result).get("finalFareId").asText();
        action(id,"fare","admin-demo","ADMIN",null).andExpect(jsonPath("$.finalFareId").value(fareId));
        action(id,"completion","driver@example.test","DRIVER","{\"actualDistanceKm\":1,\"actualDurationMinutes\":1}").andExpect(status().isConflict());
        assertThat(repository.findById(UUID.fromString(id)).orElseThrow().response().actualDistanceKm()).isEqualByComparingTo("12");
        // Driver reservation is released after completion.
        assign(create("passenger-001","Colombo").get("id").asText());
    }
    @Test void invalidTransitionsAndNoAvailableDriverDoNotCorruptState() throws Exception {
        String id=create("passenger-001","Colombo").get("id").asText();
        action(id,"start","admin-demo","ADMIN",null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        action(id,"fare","admin-demo","ADMIN",null).andExpect(status().isConflict());
        String rural=create("passenger-001","Kandy").get("id").asText();
        action(rural,"assignment","passenger-001","PASSENGER",null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NO_AVAILABLE_DRIVER"));
        assertThat(repository.findById(UUID.fromString(rural)).orElseThrow().getStatus().name()).isEqualTo("REQUESTED");
    }
    @Test void cancellationReleasesDriverAndBlocksFurtherLifecycle() throws Exception {
        String id=create("passenger-001","Colombo").get("id").asText();assign(id);
        action(id,"cancellation","passenger-001","PASSENGER","{\"reason\":\"Plans changed\"}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        action(id,"acceptance","driver@example.test","DRIVER",null).andExpect(status().isConflict());
        assign(create("passenger-001","Colombo").get("id").asText());
    }
    @Test void cannotCancelInProgressRide() throws Exception {
        String id=create("passenger-001","Colombo").get("id").asText();start(id);
        action(id,"cancellation","passenger-001","PASSENGER","{\"reason\":\"Stop\"}").andExpect(status().isConflict());
    }
    @Test void authenticationOwnershipAndAssignedDriverEnforced() throws Exception {
        mvc.perform(get("/api/rides")).andExpect(status().isUnauthorized());
        String id=create("passenger-001","Colombo").get("id").asText();assign(id);
        mvc.perform(get("/api/rides/"+id).with(user("passenger-002").roles("PASSENGER"))).andExpect(status().isForbidden());
        action(id,"cancellation","passenger-002","PASSENGER","{\"reason\":\"No\"}").andExpect(status().isForbidden());
        action(id,"acceptance","other-driver@example.test","DRIVER",null).andExpect(status().isForbidden());
        action(id,"acceptance","passenger-001","PASSENGER",null).andExpect(status().isForbidden());
        action(id,"fare","passenger-001","PASSENGER",null).andExpect(status().isForbidden());
        mvc.perform(get("/api/rides").with(user("passenger-002").roles("PASSENGER"))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/rides").with(user("driver@example.test").roles("DRIVER"))).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void validatesInputIdsPagingAndUnknownFields() throws Exception {
        for(String body:List.of("{}","{\"pickup\":\"A\",\"destination\":\"B\",\"serviceArea\":\"Colombo\",\"estimatedDistanceKm\":10,\"estimatedDurationMinutes\":20.5}",
            "{\"pickup\":\"A\",\"destination\":\"B\",\"serviceArea\":\"Colombo\",\"estimatedDistanceKm\":10,\"estimatedDurationMinutes\":20,\"passengerId\":\"victim\"}"))
            mvc.perform(post("/api/rides").with(user("passenger-001").roles("PASSENGER")).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rides/not-a-uuid").with(user("passenger-001").roles("PASSENGER"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rides?size=101").with(user("passenger-001").roles("PASSENGER"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rides/"+UUID.randomUUID()).with(user("admin-demo").roles("ADMIN"))).andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }
    @Test void oneDriverCannotBeAssignedToConcurrentRides() throws Exception {
        UUID a=UUID.fromString(create("passenger-001","Colombo").get("id").asText());
        UUID b=UUID.fromString(create("passenger-001","Colombo").get("id").asText());
        var pool=Executors.newFixedThreadPool(2);var latch=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<String>>();
            for(UUID id:List.of(a,b)) futures.add(pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("passenger-001","unused",List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))));
                try { latch.await();return service.assign(id).status().name(); }
                catch(ServiceException ex) { return ex.getCode(); }
                finally { SecurityContextHolder.clearContext(); }
            }));
            latch.countDown();
            assertThat(List.of(futures.get(0).get(10,TimeUnit.SECONDS),futures.get(1).get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("ASSIGNED","NO_AVAILABLE_DRIVER");
        } finally { pool.shutdownNow(); }
    }
    @Test void swaggerContractAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/rides/{id}/completion'].put").exists());
    }
}
