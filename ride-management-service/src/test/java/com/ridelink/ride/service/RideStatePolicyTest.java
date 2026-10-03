package com.ridelink.ride.service;

import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.exception.ServiceException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RideStatePolicyTest {
    private final RideStatePolicy policy=new RideStatePolicy();
    @Test void validatesEveryStateAgainstEveryExpectedState() {
        for(var current:RideStatus.values()) for(var expected:RideStatus.values()) {
            if(current==expected) assertThatCode(() -> policy.require(current,expected)).doesNotThrowAnyException();
            else assertThatThrownBy(() -> policy.require(current,expected)).isInstanceOf(ServiceException.class);
        }
    }
    @Test void cancellationAllowedOnlyBeforeStarting() {
        for(var current:RideStatus.values()) {
            if(current==RideStatus.REQUESTED || current==RideStatus.ASSIGNED || current==RideStatus.ACCEPTED)
                assertThatCode(() -> policy.requireCancellable(current)).doesNotThrowAnyException();
            else assertThatThrownBy(() -> policy.requireCancellable(current)).isInstanceOf(ServiceException.class);
        }
    }
}
