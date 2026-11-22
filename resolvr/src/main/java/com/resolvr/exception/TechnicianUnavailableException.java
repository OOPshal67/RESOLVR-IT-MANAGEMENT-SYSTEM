package com.resolvr.exception;

/**
 * Thrown when attempting to assign an incident to an unavailable technician.
 */
public class TechnicianUnavailableException extends RuntimeException {
    public TechnicianUnavailableException(String message) {
        super(message);
    }

    public TechnicianUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
