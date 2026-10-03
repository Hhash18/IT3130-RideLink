package com.ridelink.ride.service;

import com.ridelink.ride.dto.DriverResponse;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class DriverSelectorTest {
    @Test void filtersInvalidUnavailableWrongAreaAndNoVehicleAndSortsLowestIdFirst() {
        var candidates=Arrays.asList(
            new DriverResponse(3L,"d3@test","AVAILABLE","COLOMBO",103L),
            new DriverResponse(1L,"d1@test","AVAILABLE"," Colombo ",101L),
            new DriverResponse(2L,"d2@test","ON_TRIP","Colombo",102L),
            new DriverResponse(4L,"d4@test","AVAILABLE","Kandy",104L),
            new DriverResponse(5L,"d5@test","AVAILABLE","Colombo",null),
            new DriverResponse(6L,null,"AVAILABLE","Colombo",106L),null);
        assertThat(new DriverSelector().eligible(candidates,"Colombo")).extracting(DriverResponse::id).containsExactly(1L,3L);
    }
    @Test void returnsEmptyWhenNoDrivers() { assertThat(new DriverSelector().eligible(List.of(),"Colombo")).isEmpty(); }
}
