package com.ridelink.drivervehicle.dto;

public class VehicleResponse {

    private Long id;
    private String registrationNumber;
    private String type;
    private String model;
    private Integer capacity;
    private String status;

    public VehicleResponse() {
    }

    public VehicleResponse(
            Long id,
            String registrationNumber,
            String type,
            String model,
            Integer capacity,
            String status
    ) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.model = model;
        this.capacity = capacity;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}