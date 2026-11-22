package com.resolvr.exception;

/**
 * Thrown when a requested IT service or incident category does not exist.
 */
public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String message) {
        super(message);
    }

    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
