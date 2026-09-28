package com.ridelink.farepayment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static com.ridelink.farepayment.ApiModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"demo", "test"})
class FarePaymentApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FareRepository fares;
    @Autowired PaymentRepository payments;
    @Autowired PaymentService paymentService;
    @BeforeEach void clean() { payments.deleteAll(); fares.deleteAll(); }
    void finalizeRide() throws Exception {
        mvc.perform(put("/api/fares/rides/ride-demo-001").with(user("admin-demo").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fare.total").value(800.0));
    }
    JsonNode pay(UUID key, String simulation) throws Exception {
        String body = mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))
                        .header("Idempotency-Key", key).contentType("application/json")
                        .content("{\"method\":\"MOCK_CARD\",\"simulation\":\""+simulation+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }
    @Test void completeWorkflowReplaysWithoutDuplicateAndReturnsStableReceipt() throws Exception {
        finalizeRide(); finalizeRide(); assertThat(fares.count()).isEqualTo(1);
        mvc.perform(get("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER")))
                .andExpect(jsonPath("$.status").value("UNPAID"));
        UUID key = UUID.randomUUID(); var payment = pay(key, "SUCCESS");
        assertThat(pay(key,"SUCCESS").get("id")).isEqualTo(payment.get("id"));
        assertThat(payments.count()).isEqualTo(1);
        String id = payment.get("id").asText();
        mvc.perform(get("/api/payments/"+id+"/receipt").with(user("passenger-001").roles("PASSENGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.simulated").value(true))
                .andExpect(jsonPath("$.fare.total").value(800.0))
                .andExpect(jsonPath("$.receiptNumber").value(payment.get("receiptNumber").asText()));
        mvc.perform(get("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER")))
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))
                .header("Idempotency-Key", UUID.randomUUID()).contentType("application/json")
                .content("{\"method\":\"CASH\",\"simulation\":\"SUCCESS\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_PAID"));
    }
    @Test void failedPaymentIsRecordedCanBeRetriedAndHasNoReceipt() throws Exception {
        finalizeRide(); UUID key = UUID.randomUUID(); var failed = pay(key,"DECLINED");
        assertThat(failed.get("status").asText()).isEqualTo("FAILED");
        assertThat(pay(key,"DECLINED").get("id")).isEqualTo(failed.get("id"));
        mvc.perform(get("/api/payments/"+failed.get("id").asText()+"/receipt").with(user("passenger-001").roles("PASSENGER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_SUCCESSFUL"));
        mvc.perform(get("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER")))
                .andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))
                .header("Idempotency-Key", key).contentType("application/json")
                .content("{\"method\":\"MOCK_CARD\",\"simulation\":\"SUCCESS\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        assertThat(pay(UUID.randomUUID(),"SUCCESS").get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(payments.count()).isEqualTo(2);
    }
    @Test void requiresAuthenticationRolesAndOwnership() throws Exception {
        mvc.perform(get("/api/fares/rides/ride-demo-001")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(put("/api/fares/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/fares/rides/ride-demo-001").with(user("driver-001").roles("DRIVER"))).andExpect(status().isForbidden());
        finalizeRide();
        for (String path : List.of("/api/fares/rides/ride-demo-001", "/api/payments/rides/ride-demo-001"))
            mvc.perform(get(path).with(user("passenger-002").roles("PASSENGER"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-002").roles("PASSENGER"))
                .header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                .content("{\"method\":\"CASH\",\"simulation\":\"SUCCESS\"}"))
                .andExpect(status().isForbidden());
        var paid = pay(UUID.randomUUID(),"SUCCESS");
        for (String suffix : List.of("", "/receipt"))
            mvc.perform(get("/api/payments/"+paid.get("id").asText()+suffix).with(user("passenger-002").roles("PASSENGER")))
                    .andExpect(status().isForbidden());
    }
    @Test void validatesInputsAndRideLifecycle() throws Exception {
        mvc.perform(post("/api/fares/estimate").with(user("passenger-001").roles("PASSENGER"))
                .contentType("application/json").content("{\"pickup\":\"A\",\"destination\":\"B\",\"distanceKm\":10,\"durationMinutes\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fare.total").value(800.0));
        mvc.perform(post("/api/fares/estimate").with(user("passenger-001").roles("PASSENGER"))
                .contentType("application/json").content("{\"pickup\":\"\",\"destination\":\"B\",\"distanceKm\":-1,\"durationMinutes\":20}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/fares/rides/ride-demo-active").with(user("admin-demo").roles("ADMIN")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RIDE_NOT_COMPLETED"));
        mvc.perform(put("/api/fares/rides/missing").with(user("admin-demo").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))
                .contentType("application/json").content("{\"method\":\"CASH\",\"simulation\":\"SUCCESS\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))
                .header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                .content("{\"method\":\"CASH\",\"simulation\":\"SUCCESS\",\"amount\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/payments/not-a-uuid").with(user("passenger-001").roles("PASSENGER"))).andExpect(status().isBadRequest());
    }
    @Test void rejectsFractionalMinutesAndInvalidPaymentEnums() throws Exception {
        mvc.perform(post("/api/fares/estimate").with(user("passenger-001").roles("PASSENGER"))
                .contentType("application/json").content("{\"pickup\":\"A\",\"destination\":\"B\",\"distanceKm\":10,\"durationMinutes\":20.5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/payments/rides/ride-demo-001").with(user("passenger-001").roles("PASSENGER"))
                .header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                .content("{\"method\":\"REAL_CARD\",\"simulation\":\"SUCCESS\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/fares/rides/invalid.id").with(user("passenger-001").roles("PASSENGER")))
                .andExpect(status().isBadRequest());
    }
    @Test void openApiIsAccessibleAndContainsAllOperations() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/payments/{paymentId}/receipt'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes.demoBasic.scheme").value("basic"));
    }
    @Test void concurrentRequestsCannotPayTheSameFareTwice() throws Exception {
        finalizeRide(); var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("passenger-001", "unused", List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))));
            try {
                start.await();
                return paymentService.pay("ride-demo-001",UUID.randomUUID(),new PaymentRequest(PaymentMethod.CASH,Simulation.SUCCESS)).status().name();
            } catch (ServiceException ex) { return ex.code; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var a=pool.submit(attempt);var b=pool.submit(attempt);start.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("SUCCEEDED","ALREADY_PAID");
            assertThat(payments.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}
