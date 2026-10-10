package com.ridelink.ride.service;


import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.exception.ServiceException;
import org.springframework.stereotype.Component;
@Component
public class RideStatePolicy {
    public void require(RideStatus current,RideStatus expected) {
        if(current!=expected) throw ServiceException.conflict("INVALID_TRANSITION","Expected "+expected+" but ride is "+current);
    }
    public void requireCancellable(RideStatus current) {
        if(current!=RideStatus.REQUESTED && current!=RideStatus.ASSIGNED && current!=RideStatus.ACCEPTED)
            throw ServiceException.conflict("INVALID_TRANSITION","Only requested, assigned or accepted rides may be cancelled");
    }
}
