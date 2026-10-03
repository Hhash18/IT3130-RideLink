package com.ridelink.ride.controller;

import com.ridelink.ride.client.*;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.ServiceException;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:ride-failure-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"demo","test"})
class DependencyFailureTest {
    @Autowired RideService rides;
    @Autowired FareIntegrationService billing;
    @Autowired RideRepository repository;
    @MockitoBean FareClient fares;
    @MockitoBean DriverClient drivers;
    final RideRequest request=new RideRequest("A","B","Colombo",BigDecimal.TEN,20);
    @BeforeEach void prepare() {
        repository.deleteAll();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin-demo","unused",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(fares.estimate(any())).thenReturn(new FareQuote(new BigDecimal("800.00"),"LKR"));
        when(drivers.available()).thenReturn(List.of(new DriverResponse(1L,"driver@example.test","AVAILABLE","Colombo",101L)));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void estimateFailureDoesNotCreateRide() {
        when(fares.estimate(any())).thenThrow(ServiceException.upstream("Fare"));
        assertThatThrownBy(() -> rides.create(request)).isInstanceOf(ServiceException.class);
        assertThat(repository.count()).isZero();
    }
    @Test void assignmentFailurePreservesRequestedRide() {
        UUID id=rides.create(request).id();when(drivers.available()).thenThrow(ServiceException.upstream("Driver"));
        assertThatThrownBy(() -> rides.assign(id)).isInstanceOf(ServiceException.class);
        assertThat(rides.get(id).status().name()).isEqualTo("REQUESTED");
    }
    @Test void billingFailureKeepsCompletedRideAndRetryRunsOutsideRideTransaction() {
        UUID id=rides.create(request).id();rides.assign(id);rides.accept(id);rides.start(id);
        rides.complete(id,new CompleteRideRequest(BigDecimal.TEN,20));
        when(fares.finalizeFare(id)).thenThrow(ServiceException.upstream("Fare"));
        assertThatThrownBy(() -> billing.finalizeFare(id)).isInstanceOf(ServiceException.class);
        assertThat(rides.get(id).status().name()).isEqualTo("COMPLETED");assertThat(rides.get(id).finalFareId()).isNull();
        UUID fareId=UUID.randomUUID();
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(rides.get(id).actualDistanceKm()).isEqualByComparingTo("10");
            return new FinalFareResponse(fareId,id.toString(),new FareQuote(new BigDecimal("800.00"),"LKR"));
        }).when(fares).finalizeFare(id);
        assertThat(billing.finalizeFare(id).finalFareId()).isEqualTo(fareId);
        billing.finalizeFare(id);verify(fares,times(2)).finalizeFare(id); // failure + successful retry only
    }
    @Test void mismatchedFareResponseCannotBeSaved() {
        UUID id=rides.create(request).id();rides.assign(id);rides.accept(id);rides.start(id);
        rides.complete(id,new CompleteRideRequest(BigDecimal.TEN,20));
        when(fares.finalizeFare(id)).thenReturn(new FinalFareResponse(UUID.randomUUID(),UUID.randomUUID().toString(),new FareQuote(BigDecimal.TEN,"LKR")));
        assertThatThrownBy(() -> billing.finalizeFare(id)).isInstanceOf(ServiceException.class);
        assertThat(rides.get(id).finalFareId()).isNull();
    }
}
