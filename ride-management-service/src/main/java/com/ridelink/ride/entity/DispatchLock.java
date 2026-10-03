package com.ridelink.ride.entity;


import jakarta.persistence.*;
@Entity
@Table(name="dispatch_lock")
public class DispatchLock {
    @Id private Integer id;
    protected DispatchLock() {}
}
