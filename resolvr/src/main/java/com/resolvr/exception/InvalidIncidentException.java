package com.resolvr.exception;

/**
 * Thrown when an incident request or state is invalid.
 */
public class InvalidIncidentException extends RuntimeException {
    public InvalidIncidentException(String message) {
        super(message);
    }

    public InvalidIncidentException(String message, Throwable cause) {
        super(message, cause);
    }
}
