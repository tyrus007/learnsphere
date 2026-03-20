package com.ashique.enrollment_service.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
