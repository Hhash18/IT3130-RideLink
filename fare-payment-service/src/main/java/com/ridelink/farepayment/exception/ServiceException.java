package com.ridelink.farepayment.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public ServiceException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public static ServiceException notFound(String message) {
        return new ServiceException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
    public static ServiceException conflict(String code, String message) {
        return new ServiceException(HttpStatus.CONFLICT, code, message);
    }
}
