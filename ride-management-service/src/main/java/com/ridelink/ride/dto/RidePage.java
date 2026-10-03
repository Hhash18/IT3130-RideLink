package com.ridelink.ride.dto;


import java.util.List;
public record RidePage(List<RideResponse> content,int page,int size,long totalElements,int totalPages) {}
