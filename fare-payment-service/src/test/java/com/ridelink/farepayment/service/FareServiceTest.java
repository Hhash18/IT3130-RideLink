package com.ridelink.farepayment.service;

import com.ridelink.farepayment.client.RideClient;
import com.ridelink.farepayment.dto.RideSnapshot;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.exception.ServiceException;
import com.ridelink.farepayment.repository.FareRepository;
import com.ridelink.farepayment.security.AccessControl;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FareServiceTest {
    final FareRepository repository = mock(FareRepository.class);
    final RideClient client = mock(RideClient.class);
    final FareService service = new FareService(repository, client, new FareCalculator(), new AccessControl());
    @Test void rejectsIncompleteRide() {
        when(client.getRide("r1")).thenReturn(new RideSnapshot("r1", "p1", "IN_PROGRESS", "A", "B", BigDecimal.ONE, 2));
        assertThatThrownBy(() -> service.finalizeFare("r1")).isInstanceOfSatisfying(ServiceException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("RIDE_NOT_COMPLETED"));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void rejectsMismatchedOrInvalidUpstreamData() {
        when(client.getRide("r1")).thenReturn(new RideSnapshot("r2", "p1", "COMPLETED", "A", "B", BigDecimal.ONE, 2));
        assertThatThrownBy(() -> service.finalizeFare("r1")).isInstanceOfSatisfying(ServiceException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("INVALID_RIDE_RESPONSE"));
        when(client.getRide("r1")).thenReturn(new RideSnapshot("r1", "p1", "COMPLETED", "A", "B", null, 2));
        assertThatThrownBy(() -> service.finalizeFare("r1")).isInstanceOf(ServiceException.class);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void usesExistingSnapshotWithoutRepricingOrNetworkCall() {
        var ride = new RideSnapshot("r1", "p1", "COMPLETED", "A", "B", BigDecimal.TEN, 20);
        var fare = new Fare(ride, new FareCalculator().calculate(BigDecimal.TEN,20));
        when(repository.findByRideId("r1")).thenReturn(Optional.of(fare));
        assertThat(service.finalizeFare("r1").id()).isEqualTo(fare.getId());
        verifyNoInteractions(client);
    }
}
