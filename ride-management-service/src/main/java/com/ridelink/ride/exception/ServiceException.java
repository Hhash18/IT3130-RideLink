package com.ridelink.ride.exception;


import org.springframework.http.HttpStatus;
public class ServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public ServiceException(HttpStatus status,String code,String message) { super(message);this.status=status;this.code=code; }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public static ServiceException notFound(String message) { return new ServiceException(HttpStatus.NOT_FOUND,"NOT_FOUND",message); }
    public static ServiceException conflict(String code,String message) { return new ServiceException(HttpStatus.CONFLICT,code,message); }
    public static ServiceException upstream(String name) { return new ServiceException(HttpStatus.SERVICE_UNAVAILABLE,"DEPENDENCY_UNAVAILABLE",name+" service unavailable; retry later"); }
    public static ServiceException invalid(String name) { return new ServiceException(HttpStatus.BAD_GATEWAY,"INVALID_DEPENDENCY_RESPONSE",name+" service returned invalid data"); }
}
