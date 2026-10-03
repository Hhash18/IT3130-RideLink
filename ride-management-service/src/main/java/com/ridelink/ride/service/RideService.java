package com.ridelink.ride.service;


import com.ridelink.ride.client.*;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.entity.*;
import com.ridelink.ride.exception.ServiceException;
import com.ridelink.ride.repository.*;
import com.ridelink.ride.security.AccessControl;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class RideService {
    private final RideRepository rides;
    private final DispatchLockRepository dispatch;
    private final DriverClient drivers;
    private final FareClient fares;
    private final DriverSelector selector;
    private final FareValidator fareValidator;
    private final RideStatePolicy states;
    private final AccessControl access;
    public RideService(RideRepository rides,DispatchLockRepository dispatch,DriverClient drivers,FareClient fares,
            DriverSelector selector,FareValidator fareValidator,RideStatePolicy states,AccessControl access) {
        this.rides=rides;this.dispatch=dispatch;this.drivers=drivers;this.fares=fares;
        this.selector=selector;this.fareValidator=fareValidator;this.states=states;this.access=access;
    }
    @Transactional
    public RideResponse create(RideRequest request) {
        var quote=fares.estimate(request);fareValidator.validate(quote);
        return rides.saveAndFlush(new Ride(access.subject(),request,quote)).response();
    }
    @Transactional(readOnly=true)
    public RideResponse get(UUID id) { var ride=find(id);access.reader(ride);return ride.response(); }
    @Transactional(readOnly=true)
    public RidePage list(int page,int size) {
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt").and(Sort.by("id")));
        Page<Ride> result=access.privileged() ? rides.findAll(pageable)
                : access.hasRole("DRIVER") ? rides.findByDriverEmailIgnoreCase(access.driverEmail(),pageable)
                : rides.findByPassengerId(access.subject(),pageable);
        return new RidePage(result.getContent().stream().map(Ride::response).toList(),page,size,result.getTotalElements(),result.getTotalPages());
    }
    @Transactional
    public RideResponse assign(UUID id) {
        // Serializes dispatch decisions in this service/database, also across service instances.
        dispatch.acquire();
        var ride=locked(id);access.ownerOrAdmin(ride);states.require(ride.getStatus(),RideStatus.REQUESTED);
        var available=drivers.available();
        if(available==null) throw ServiceException.invalid("Driver");
        var chosen=selector.eligible(available,ride.getServiceArea()).stream()
            .filter(d -> !rides.existsByActiveDriverIdOrActiveVehicleId(d.id(),d.vehicleId())).findFirst()
            .orElseThrow(() -> ServiceException.conflict("NO_AVAILABLE_DRIVER","No eligible unassigned driver in this service area"));
        ride.assign(chosen);return rides.saveAndFlush(ride).response();
    }
    @Transactional
    public RideResponse accept(UUID id) { var ride=locked(id);access.driverOrAdmin(ride);states.require(ride.getStatus(),RideStatus.ASSIGNED);ride.accept();return ride.response(); }
    @Transactional
    public RideResponse start(UUID id) { var ride=locked(id);access.driverOrAdmin(ride);states.require(ride.getStatus(),RideStatus.ACCEPTED);ride.start();return ride.response(); }
    @Transactional
    public RideResponse complete(UUID id,CompleteRideRequest request) {
        var ride=locked(id);access.driverOrAdmin(ride);states.require(ride.getStatus(),RideStatus.IN_PROGRESS);
        ride.complete(request);return ride.response();
    }
    @Transactional
    public RideResponse cancel(UUID id,CancelRideRequest request) {
        var ride=locked(id);access.ownerOrAdmin(ride);states.requireCancellable(ride.getStatus());ride.cancel(request.reason());return ride.response();
    }
    @Transactional(readOnly=true)
    public RideResponse completedForBilling(UUID id) {
        var ride=find(id);states.require(ride.getStatus(),RideStatus.COMPLETED);return ride.response();
    }
    @Transactional
    public RideResponse recordFare(UUID id,FinalFareResponse fare) {
        var ride=locked(id);states.require(ride.getStatus(),RideStatus.COMPLETED);fareValidator.validate(fare.fare());
        if(fare.id()==null || !id.toString().equals(fare.rideId())) throw ServiceException.invalid("Fare");
        var existing=ride.response();
        if(existing.finalFareId()!=null) {
            if(!existing.finalFareId().equals(fare.id()) || existing.finalFare().compareTo(fare.fare().total())!=0)
                throw ServiceException.conflict("FARE_MISMATCH","Final fare is already recorded with different values");
            return existing;
        }
        ride.recordFare(fare);return ride.response();
    }
    private Ride find(UUID id) { return rides.findById(id).orElseThrow(() -> ServiceException.notFound("Ride not found")); }
    private Ride locked(UUID id) { return rides.lockById(id).orElseThrow(() -> ServiceException.notFound("Ride not found")); }
}
