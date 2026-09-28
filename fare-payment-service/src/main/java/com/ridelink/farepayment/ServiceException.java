package com.ridelink.farepayment;


import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {
    final HttpStatus status;
    final String code;
    public ServiceException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    static ServiceException notFound(String message) {
        return new ServiceException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
    static ServiceException conflict(String code, String message) {
        return new ServiceException(HttpStatus.CONFLICT, code, message);
    }
}
