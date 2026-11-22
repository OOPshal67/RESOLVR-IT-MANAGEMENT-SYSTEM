package com.resolvr.exception;

/**
 * Thrown when a technician has reached their maximum active incident capacity.
 */
public class TechnicianWorkloadExceededException extends RuntimeException {
    public TechnicianWorkloadExceededException(String message) {
        super(message);
    }

    public TechnicianWorkloadExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
